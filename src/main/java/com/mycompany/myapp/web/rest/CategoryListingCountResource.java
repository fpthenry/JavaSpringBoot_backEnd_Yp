package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.service.CategoryListingCountService;
import com.mycompany.myapp.service.CategoryListingCountService.Status;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quản trị: tính lại số doanh nghiệp của mỗi ngành ({@code category.listing_count}). Chỉ ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/admin/categories")
@PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
@Tag(name = "category-admin", description = "Tính lại số doanh nghiệp theo ngành nghề (ROLE_ADMIN)")
public class CategoryListingCountResource {

    private static final Logger LOG = LoggerFactory.getLogger(CategoryListingCountResource.class);

    private final CategoryListingCountService countService;

    public CategoryListingCountResource(CategoryListingCountService countService) {
        this.countService = countService;
    }

    @PostMapping("/listing-counts")
    @Operation(
        summary = "Tính lại số doanh nghiệp của mỗi ngành (chạy nền)",
        description = "Đếm doanh nghiệp đã xuất bản của mỗi ngành, tính cả ngành con, mỗi doanh nghiệp một lần; ghi vào " +
            "listingCount, rồi reindex Elasticsearch cho category. Đọc ~25 triệu liên kết, mất khoảng 1 phút. Trả 202 ngay; " +
            "theo dõi bằng GET cùng đường dẫn. Đang chạy thì 409. Cần chạy lại sau khi đồng bộ dữ liệu."
    )
    public ResponseEntity<Status> start() {
        LOG.info("REST request to recompute category listing counts");
        try {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(countService.start());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(countService.getStatus());
        }
    }

    @GetMapping("/listing-counts")
    @Operation(summary = "Trạng thái job tính số doanh nghiệp theo ngành")
    public Status getStatus() {
        return countService.getStatus();
    }
}
