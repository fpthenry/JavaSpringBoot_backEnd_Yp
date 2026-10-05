package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.service.wordpress.WordPressPostSyncService;
import com.mycompany.myapp.service.wordpress.WordPressPostSyncService.SyncStatus;
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
 * Quản trị: đồng bộ bài viết, danh mục, thẻ từ WordPress cũ. Chỉ ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/admin/wordpress")
@PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
@Tag(name = "wordpress-sync", description = "Đồng bộ bài viết, danh mục, thẻ từ WordPress yp.com.vn (ROLE_ADMIN)")
public class WordPressSyncResource {

    private static final Logger LOG = LoggerFactory.getLogger(WordPressSyncResource.class);

    private final WordPressPostSyncService syncService;

    public WordPressSyncResource(WordPressPostSyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/sync-posts")
    @Operation(
        summary = "Bắt đầu đồng bộ bài viết từ WordPress (chạy nền)",
        description = "Đọc REST API công khai của WordPress: danh mục → thẻ → bài viết đã xuất bản. Thêm mới hoặc cập nhật theo id " +
            "WordPress, không xóa gì; chạy lại nhiều lần được. Xong thì tự reindex Elasticsearch cho blogcategory, tag, blogpost. " +
            "Trả 202 ngay; xem tiến độ bằng GET /api/admin/wordpress/sync-posts. 409 nếu đang có job chạy."
    )
    public ResponseEntity<SyncStatus> startSync() {
        LOG.info("REST request to sync posts from WordPress");
        try {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(syncService.start());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(syncService.getStatus());
        }
    }

    @GetMapping("/sync-posts")
    @Operation(summary = "Trạng thái và tiến độ đồng bộ WordPress")
    public SyncStatus getSyncStatus() {
        return syncService.getStatus();
    }
}
