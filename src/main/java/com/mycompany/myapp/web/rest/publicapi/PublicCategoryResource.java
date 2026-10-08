package com.mycompany.myapp.web.rest.publicapi;

import com.mycompany.myapp.service.PublicCategoryService;
import com.mycompany.myapp.service.dto.publicapi.PublicCategoryDTO;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API công khai ngành nghề cho FE (Next.js): mục lục theo chữ cái. Cần header {@code X-API-Key}.
 */
@RestController
@RequestMapping("/api/public/v1/categories")
@Tag(name = "public-category", description = "API công khai: ngành nghề, mục lục theo chữ cái (cần header X-API-Key)")
public class PublicCategoryResource {

    /** Ngành nghề ít thay đổi: FE cache 5 phút. */
    private static final CacheControl JSON_CACHE = CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic();

    private final PublicCategoryService categoryService;

    public PublicCategoryResource(PublicCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/letters")
    @Operation(
        summary = "Các chữ cái của mục lục ngành nghề",
        description = "Mỗi chữ cái có ngành, kèm số ngành, theo thứ tự A–Z; '#' (tên không bắt đầu bằng chữ cái) ở cuối. " +
            "Chữ có dấu gộp vào chữ gốc (Ô → O, Đ → D). Dùng cho dãy nút chữ cái."
    )
    public ResponseEntity<List<PublicCategoryDTO.Letter>> getLetters(
        @Parameter(description = "true: chỉ đếm ngành đã có doanh nghiệp") @RequestParam(
            name = "hideEmpty",
            defaultValue = "false"
        ) boolean hideEmpty
    ) {
        return ResponseEntity.ok().cacheControl(JSON_CACHE).body(categoryService.letters(hideEmpty));
    }

    @GetMapping
    @Operation(
        summary = "Ngành nghề theo chữ cái đầu và/hoặc từ khóa, kèm số doanh nghiệp",
        description = "letter: một chữ cái (l = L, Đ = D) hoặc '#'; bỏ trống = tất cả. q: từ khóa trong tên ngành, không phân " +
            "biệt dấu và hoa thường. listingCount: số doanh nghiệp đã xuất bản, tính cả ngành con. Mặc định sắp theo tên " +
            "(A–Z tiếng Việt); sort=listingCount,desc để ngành nhiều doanh nghiệp lên trước. size tối đa 500."
    )
    public ResponseEntity<PublicPageDTO<PublicCategoryDTO>> getCategories(
        @Parameter(description = "Chữ cái đầu, ví dụ L") @RequestParam(name = "letter", required = false) String letter,
        @Parameter(description = "Từ khóa trong tên ngành, ví dụ lap dat") @RequestParam(name = "q", required = false) String q,
        @Parameter(description = "true: bỏ ngành chưa có doanh nghiệp") @RequestParam(
            name = "hideEmpty",
            defaultValue = "false"
        ) boolean hideEmpty,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok()
            .cacheControl(JSON_CACHE)
            .body(categoryService.categories(letter, q, hideEmpty, pageable));
    }
}
