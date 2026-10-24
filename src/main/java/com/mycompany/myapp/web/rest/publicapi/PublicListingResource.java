package com.mycompany.myapp.web.rest.publicapi;

import com.mycompany.myapp.service.PublicListingService;
import com.mycompany.myapp.service.dto.publicapi.PublicListingDTO;
import com.mycompany.myapp.service.dto.publicapi.PublicPageDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API công khai doanh nghiệp cho FE (Next.js), thay Elasticsearch của WordPress. Cần header {@code X-API-Key}. Chỉ trả
 * doanh nghiệp đã xuất bản.
 */
@RestController
@RequestMapping("/api/public/v1/listings")
@Tag(name = "public-listing", description = "API công khai: doanh nghiệp — tìm, lọc theo ngành/địa phương, chi tiết (cần header X-API-Key)")
public class PublicListingResource {

    /** Danh sách, chi tiết: FE cache 1 phút (sửa doanh nghiệp trong trang quản trị tối đa chừng ấy mới hiện). */
    private static final CacheControl JSON_CACHE = CacheControl.maxAge(Duration.ofMinutes(1)).cachePublic();

    private final PublicListingService listingService;

    public PublicListingResource(PublicListingService listingService) {
        this.listingService = listingService;
    }

    @GetMapping
    @Operation(
        summary = "Danh sách doanh nghiệp: tìm theo từ khóa, lọc theo ngành nghề, địa phương, nổi bật",
        description = "q: từ khóa, mỗi chữ phải có trong tên, địa chỉ (cũ và mới), điện thoại, email, website, mã số thuế, " +
            "người đại diện hoặc giới thiệu; tên được ưu tiên. category: slug hoặc id ngành (lấy cả ngành con). location: " +
            "slug (nhận cả slug cũ của website WordPress) hoặc id địa phương (lấy cả đơn vị con). Ngành/địa phương không có → 404. " +
            "featured=true: chỉ doanh nghiệp nổi bật. Không có sort: có q thì theo độ liên quan, không thì mới đăng trước; " +
            "sort nhận publishedAt, modifiedAt, viewCount, name, id. size tối đa 50. Danh sách chỉ có đoạn đầu description (≤ 300 ký tự), không có images " +
            "(lấy ở API chi tiết)."
    )
    public ResponseEntity<PublicPageDTO<PublicListingDTO>> getListings(
        @Parameter(description = "Từ khóa, ví dụ bia phan") @RequestParam(name = "q", required = false) String q,
        @Parameter(description = "Slug hoặc id ngành nghề, ví dụ bom-chan-khong-779") @RequestParam(
            name = "category",
            required = false
        ) String category,
        @Parameter(description = "Slug hoặc id địa phương, ví dụ thanh-pho-ha-noi") @RequestParam(
            name = "location",
            required = false
        ) String location,
        @Parameter(description = "true: chỉ doanh nghiệp nổi bật") @RequestParam(name = "featured", required = false) Boolean featured,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok()
            .cacheControl(JSON_CACHE)
            .body(listingService.listings(q, category, location, featured, pageable));
    }

    @GetMapping("/suggest")
    @Operation(
        summary = "Gợi ý tên doanh nghiệp khi gõ ô tìm kiếm",
        description = "Tên khớp mọi chữ đã gõ, chữ cuối chỉ cần khớp đầu từ. Trả id, tên, slug. size tối đa 20 (mặc định 10)."
    )
    public ResponseEntity<List<PublicListingDTO.Suggestion>> suggest(
        @Parameter(description = "Chữ đang gõ, ví dụ cong ty bia ph") @RequestParam(name = "q", required = false) String q,
        @Parameter(description = "Số gợi ý") @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return ResponseEntity.ok().cacheControl(JSON_CACHE).body(listingService.suggest(q, size));
    }

    @GetMapping("/wp/{wpId}")
    @Operation(
        summary = "Chi tiết doanh nghiệp theo id bài WordPress cũ",
        description = "Cho URL cũ dạng ?p=913645 hoặc liên kết lưu theo post_id: FE đọc slug rồi chuyển hướng 301 sang " +
            "/listing/{slug}. 404 nếu không có hoặc là bản nháp."
    )
    public ResponseEntity<PublicListingDTO> getListingByWpId(@PathVariable("wpId") long wpId) {
        return listingService
            .listingByWpId(wpId)
            .map(listing -> ResponseEntity.ok().cacheControl(JSON_CACHE).body(listing))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{slug}")
    @Operation(
        summary = "Chi tiết doanh nghiệp theo slug",
        description = "Đủ thông tin cho trang /listing/{slug}: giới thiệu, liên hệ, địa chỉ cũ và mới, tọa độ (nguyên văn), " +
            "ảnh, ngành nghề, địa phương. 404 nếu không có hoặc là bản nháp (FE trả 404 thật cho SEO)."
    )
    public ResponseEntity<PublicListingDTO> getListing(@PathVariable("slug") String slug) {
        return listingService
            .listing(slug)
            .map(listing -> ResponseEntity.ok().cacheControl(JSON_CACHE).body(listing))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{slug}/related")
    @Operation(
        summary = "Doanh nghiệp liên quan",
        description = "Cùng ngành nghề (ngành gắn trực tiếp), cùng tỉnh lên trước, rồi nổi bật, rồi mới đăng; không gồm chính nó. " +
            "size tối đa 30 (mặc định 12). 404 nếu không có doanh nghiệp."
    )
    public ResponseEntity<List<PublicListingDTO>> getRelated(
        @PathVariable("slug") String slug,
        @Parameter(description = "Số doanh nghiệp") @RequestParam(name = "size", defaultValue = "12") int size
    ) {
        return listingService
            .related(slug, size)
            .map(related -> ResponseEntity.ok().cacheControl(JSON_CACHE).body(related))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
