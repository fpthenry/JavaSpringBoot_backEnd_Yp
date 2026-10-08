package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.service.PublicCategoryService;
import com.mycompany.myapp.service.dto.publicapi.PublicCategoryDTO;
import com.mycompany.myapp.service.dto.publicapi.PublicPageDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mục lục ngành nghề theo chữ cái cho trang quản trị (đăng nhập JWT). Cùng dữ liệu và cách lọc với API công khai
 * {@code /api/public/v1/categories} ({@link PublicCategoryService}), nhưng không cần khóa API của FE.
 */
@RestController
@RequestMapping("/api/category-index")
@Tag(name = "category-index", description = "Mục lục ngành nghề theo chữ cái, kèm số doanh nghiệp (trang quản trị)")
public class CategoryIndexResource {

    private final PublicCategoryService categoryService;

    public CategoryIndexResource(PublicCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/letters")
    @Operation(
        summary = "Các chữ cái của mục lục ngành nghề",
        description = "Mỗi chữ cái có ngành, kèm số ngành, A–Z rồi '#'. Chữ có dấu gộp vào chữ gốc (Ô → O, Đ → D)."
    )
    public List<PublicCategoryDTO.Letter> getLetters(
        @Parameter(description = "true: chỉ đếm ngành đã có doanh nghiệp") @RequestParam(
            name = "hideEmpty",
            defaultValue = "false"
        ) boolean hideEmpty
    ) {
        return categoryService.letters(hideEmpty);
    }

    @GetMapping
    @Operation(
        summary = "Ngành nghề theo chữ cái đầu và/hoặc tên, kèm số doanh nghiệp",
        description = "letter: một chữ cái hoặc '#'; q: từ khóa trong tên, không phân biệt dấu; sort: name (mặc định) " +
            "hoặc listingCount; size tối đa 500."
    )
    public PublicPageDTO<PublicCategoryDTO> getCategories(
        @Parameter(description = "Chữ cái đầu, ví dụ L") @RequestParam(name = "letter", required = false) String letter,
        @Parameter(description = "Từ khóa trong tên ngành") @RequestParam(name = "q", required = false) String q,
        @Parameter(description = "true: bỏ ngành chưa có doanh nghiệp") @RequestParam(
            name = "hideEmpty",
            defaultValue = "false"
        ) boolean hideEmpty,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        return categoryService.categories(letter, q, hideEmpty, pageable);
    }
}
