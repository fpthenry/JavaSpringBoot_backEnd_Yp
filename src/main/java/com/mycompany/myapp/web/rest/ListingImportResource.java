package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.domain.ListingImportBatch;
import com.mycompany.myapp.domain.ListingImportRow;
import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.service.listingimport.ApiRecordSource;
import com.mycompany.myapp.service.listingimport.ImportRecord;
import com.mycompany.myapp.service.listingimport.ListingImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;

/**
 * Quản trị: nhập (clone) doanh nghiệp từ nguồn ngoài, kiểm tra trùng, duyệt. Chỉ ROLE_ADMIN.
 * Quy trình: (1) nhập — nên chạy thử trước ({@code dryRun=true}); (2) xem kết quả, duyệt dòng nghi trùng;
 * (3) xuất bản doanh nghiệp mới của đợt.
 */
@RestController
@RequestMapping("/api/admin/listing-imports")
@PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
@Tag(
    name = "listing-import",
    description = "Nhập doanh nghiệp từ file Excel/CSV, API bên thứ ba; kiểm tra trùng, duyệt, xuất bản (ROLE_ADMIN)"
)
public class ListingImportResource {

    private static final Logger LOG = LoggerFactory.getLogger(ListingImportResource.class);

    private final ListingImportService importService;

    public ListingImportResource(ListingImportService importService) {
        this.importService = importService;
    }

    // ---------------------------------------------------------------- nhập

    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Nhập từ file Excel (.xlsx) hoặc CSV (chạy nền)",
        description = "Dòng đầu là tiêu đề cột (xem GET /template.csv). Trùng MST / id nguồn → bổ sung ô còn trống; " +
            "nghi trùng (tên gần giống + SĐT/địa chỉ) → chờ duyệt; còn lại → tạo doanh nghiệp nháp. " +
            "Trả 202 kèm đợt nhập; xem tiến độ ở GET /{id}. 409 nếu đang có đợt khác chạy."
    )
    public ResponseEntity<ListingImportBatch> importFile(
        @RequestPart("file") MultipartFile file,
        @Parameter(description = "Chạy thử: chỉ phân loại, không ghi vào listing") @RequestParam(defaultValue = "false") boolean dryRun
    ) throws IOException {
        LOG.info("REST request to import listings from file {} ({} bytes, dryRun={})", file.getOriginalFilename(), file.getSize(), dryRun);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
            importService.startFile(file.getInputStream(), file.getOriginalFilename(), dryRun)
        );
    }

    @PostMapping("/api")
    @Operation(
        summary = "Nhập từ API JSON của bên thứ ba (chạy nền)",
        description = "Backend gọi URL (GET), lấy mảng doanh nghiệp ở itemsPath, lần lượt từng trang nếu có pageParam. " +
            "Khóa JSON nhận như tên cột file, hoặc chỉ định bằng fieldMapping. Doanh nghiệp tạo mới có source = api:<source>."
    )
    public ResponseEntity<ListingImportBatch> importApi(@RequestBody ApiRecordSource.Request request) {
        LOG.info("REST request to import listings from API {} ({})", request.source(), request.url());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(importService.startApi(request));
    }

    @Schema(description = "Danh sách doanh nghiệp gửi thẳng")
    public record RecordsRequest(
        @Schema(
            description = "Mã nguồn, ghi vào listing.source = json:<mã>",
            example = "doi-tac-a",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        String source,
        @Schema(
            description = "Doanh nghiệp: khóa theo tên cột chuẩn (name, taxCode, phone…) hoặc tiếng Việt như file",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        List<Map<String, Object>> records,
        @Schema(description = "Chạy thử") Boolean dryRun
    ) {}

    @PostMapping("/records")
    @Operation(summary = "Nhập danh sách JSON gửi thẳng (tối đa 10.000 bản ghi, chạy nền)")
    public ResponseEntity<ListingImportBatch> importRecords(@RequestBody RecordsRequest request) {
        LOG.info(
            "REST request to import {} JSON listings from {}",
            request.records() == null ? 0 : request.records().size(),
            request.source()
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
            importService.startRecords(request.source(), request.records(), Boolean.TRUE.equals(request.dryRun()))
        );
    }

    @GetMapping(value = "/template.csv", produces = "text/csv")
    @Operation(summary = "File CSV mẫu (tiêu đề cột chuẩn + 1 dòng ví dụ)")
    public ResponseEntity<byte[]> template() {
        String header = String.join(",", ImportRecord.FIELDS);
        String example =
            // thứ tự theo ImportRecord.FIELDS; tọa độ dùng cột coordinate (để trống latitude, longitude) hoặc ngược lại
            "DN-001,Công ty TNHH Ví Dụ,Example Co. Ltd,VIDU,0101234567,(024) 3846 0171,0912345678,info@vidu.vn,vidu.vn," +
            "\"Số 1 Tràng Tiền, Phường Tràng Tiền, Quận Hoàn Kiếm, Thành phố Hà Nội\"," +
            "\"Số 1 Tràng Tiền, Phường Hoàn Kiếm, Thành phố Hà Nội\",Hà Nội,Quận Hoàn Kiếm,Phường Hoàn Kiếm,\"21.0245,105.8562\",,," +
            "Nguyễn Văn A,5.000.000.000,01/01/2015,Công ty TNHH 2 thành viên,Đang hoạt động,4690,\"4690; 4610\"," +
            "Thuế cơ sở 1 thành phố Hà Nội,Giới thiệu ngắn,https://vidu.vn/logo.png,\"https://vidu.vn/anh-1.jpg; https://vidu.vn/anh-2.jpg\"";
        byte[] bom = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF }; // Excel mở đúng tiếng Việt
        byte[] body = (header + "\r\n" + example + "\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, out, 0, bom.length);
        System.arraycopy(body, 0, out, bom.length, body.length);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mau-nhap-doanh-nghiep.csv\"").body(out);
    }

    // ---------------------------------------------------------------- xem

    @GetMapping("")
    @Operation(summary = "Các đợt nhập, mới nhất trước")
    public ResponseEntity<List<ListingImportBatch>> batches(
        @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ListingImportBatch> page = importService.batches(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/running")
    @Operation(summary = "Đợt đang chạy (204 nếu không có)")
    public ResponseEntity<ListingImportBatch> running() {
        return importService
            .runningBatchId()
            .flatMap(importService::batch)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Một đợt nhập: trạng thái, tiến độ, đếm kết quả")
    public ResponseEntity<ListingImportBatch> batch(@PathVariable long id) {
        return importService
            .batch(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/rows")
    @Operation(
        summary = "Các dòng của đợt, kèm dữ liệu gốc và doanh nghiệp bị khớp",
        description = "Lọc status=SUSPECT để duyệt nghi trùng (hai bản ghi cạnh nhau: data và matched)."
    )
    public ResponseEntity<List<ListingImportService.RowView>> rows(
        @PathVariable long id,
        @RequestParam(required = false) ListingImportRow.Status status,
        @PageableDefault(size = 50, sort = "rowNo") Pageable pageable
    ) {
        Page<ListingImportService.RowView> page = importService.rows(id, status, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    // ---------------------------------------------------------------- duyệt

    @Schema(description = "Quyết định cho một dòng nghi trùng")
    public record ResolveRequest(
        @Schema(
            description = "MERGE: gộp (bổ sung ô trống); CREATE: tạo mới (nháp); SKIP: bỏ qua",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        ListingImportService.Action action,
        @Schema(description = "Gộp vào doanh nghiệp khác doanh nghiệp hệ thống đề xuất (tùy chọn, chỉ với MERGE)") Long listingId
    ) {}

    @PostMapping("/rows/{rowId}/resolve")
    @Operation(summary = "Duyệt một dòng nghi trùng: gộp, tạo mới hoặc bỏ qua")
    public ListingImportRow resolve(@PathVariable long rowId, @RequestBody ResolveRequest request) {
        if (request.action() == null) {
            throw new IllegalArgumentException("action: MERGE, CREATE hoặc SKIP");
        }
        return importService.resolve(rowId, request.action(), request.listingId());
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Xuất bản doanh nghiệp nháp do đợt tạo (hiện lên website)", description = "Trả số doanh nghiệp đã xuất bản.")
    public Map<String, Integer> publish(@PathVariable long id) {
        return Map.of("published", importService.publish(id));
    }

    // ---------------------------------------------------------------- lỗi

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, String>> conflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }
}
