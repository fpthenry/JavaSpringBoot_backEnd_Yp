package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.service.dvhc.AdministrativeUnitService;
import com.mycompany.myapp.service.dvhc.AdministrativeUnitService.LocationFixReport;
import com.mycompany.myapp.service.dvhc.ListingAddressService;
import com.mycompany.myapp.service.dvhc.ListingLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quản trị đơn vị hành chính sau sáp nhập 01/7/2025. Chỉ ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/admin/administrative-units")
@PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
@Tag(
    name = "administrative-unit-admin",
    description = "Đơn vị hành chính sau sáp nhập 2025: sửa dữ liệu theo danh mục chính thức (ROLE_ADMIN)"
)
public class AdministrativeUnitResource {

    private static final Logger LOG = LoggerFactory.getLogger(AdministrativeUnitResource.class);

    private final AdministrativeUnitService administrativeUnitService;
    private final ListingLinkService listingLinkService;
    private final ListingAddressService listingAddressService;

    public AdministrativeUnitResource(
        AdministrativeUnitService administrativeUnitService,
        ListingLinkService listingLinkService,
        ListingAddressService listingAddressService
    ) {
        this.administrativeUnitService = administrativeUnitService;
        this.listingLinkService = listingLinkService;
        this.listingAddressService = listingAddressService;
    }

    @PostMapping("/fix-locations")
    @Operation(
        summary = "Sửa bộ đơn vị mới (2025) trong bảng location theo danh mục chính thức",
        description = "Theo bảng location_conversion (nạp từ file Excel): điền mã cho bộ cũ; với bộ mới sửa mã, tên, slug, " +
            "type = ward cho xã, thêm xã còn thiếu. Không xóa gì; đơn vị không khớp được liệt kê ở unmatched. " +
            "dryRun=true (mặc định): chỉ xem trước. Chạy lại sau mỗi lần đồng bộ location từ jhipster_vnyp."
    )
    public LocationFixReport fixLocations(
        @Parameter(description = "true: chỉ xem trước, không ghi") @RequestParam(name = "dryRun", defaultValue = "true") boolean dryRun
    ) {
        LOG.info("REST request to fix 2025 administrative units, dryRun={}", dryRun);
        return administrativeUnitService.fixLocations(dryRun);
    }

    @PostMapping("/link-listings")
    @Operation(
        summary = "Gắn doanh nghiệp vào đơn vị hành chính mới (chạy nền)",
        description = "Thêm liên kết tới xã/tỉnh mới (từ location_json, không có thì qua bảng chuyển đổi), và tới huyện/tỉnh cũ " +
            "cho doanh nghiệp chưa có đơn vị cũ. Chỉ thêm, không xóa liên kết nào; chạy lại được. dryRun=true (mặc định): chỉ đếm. " +
            "Trả 202 ngay, theo dõi bằng GET cùng đường dẫn; đang chạy thì 409. Chạy sau fix-locations."
    )
    public ResponseEntity<ListingLinkService.Status> linkListings(
        @Parameter(description = "true: chỉ đếm, không ghi") @RequestParam(name = "dryRun", defaultValue = "true") boolean dryRun
    ) {
        LOG.info("REST request to link listings to 2025 administrative units, dryRun={}", dryRun);
        try {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(listingLinkService.start(dryRun));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(listingLinkService.getStatus());
        }
    }

    @PostMapping("/new-addresses")
    @Operation(
        summary = "Tạo địa chỉ mới (address_new) cho doanh nghiệp (chạy nền)",
        description = "Giữ số nhà, đường, khu của địa chỉ cũ; bỏ phường, quận, tỉnh cũ ở cuối; ghép xã mới, tỉnh mới (từ liên kết " +
            "đơn vị mới). Để trống khi địa chỉ đã là địa chỉ mới hoặc chưa biết xã mới. dryRun=true (mặc định): chỉ đếm. " +
            "overwrite=false: không ghi đè address_new đang có (giữ bản sửa tay). Chạy sau link-listings; xong thì reindex listing."
    )
    public ResponseEntity<ListingAddressService.Status> createNewAddresses(
        @Parameter(description = "true: chỉ đếm, không ghi") @RequestParam(name = "dryRun", defaultValue = "true") boolean dryRun,
        @Parameter(description = "false: chỉ điền dòng đang trống") @RequestParam(
            name = "overwrite",
            defaultValue = "true"
        ) boolean overwrite
    ) {
        LOG.info("REST request to create 2025 addresses, dryRun={}, overwrite={}", dryRun, overwrite);
        try {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(listingAddressService.start(dryRun, overwrite));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(listingAddressService.getStatus());
        }
    }

    @GetMapping("/new-addresses")
    @Operation(summary = "Trạng thái job tạo địa chỉ mới")
    public ListingAddressService.Status getNewAddressesStatus() {
        return listingAddressService.getStatus();
    }

    @GetMapping("/link-listings")
    @Operation(summary = "Trạng thái job gắn doanh nghiệp vào đơn vị hành chính mới")
    public ListingLinkService.Status getLinkListingsStatus() {
        return listingLinkService.getStatus();
    }
}
