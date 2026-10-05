package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.service.ElasticsearchReindexService;
import com.mycompany.myapp.service.ElasticsearchReindexService.IndexCheck;
import com.mycompany.myapp.service.ElasticsearchReindexService.ReindexStatus;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
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
 * Quản trị Elasticsearch: reindex dữ liệu từ MySQL. Chỉ ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/admin/elasticsearch")
@PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
@Tag(name = "elasticsearch-admin", description = "Reindex Elasticsearch từ MySQL (ROLE_ADMIN)")
public class ElasticsearchReindexResource {

    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchReindexResource.class);

    private final ElasticsearchReindexService reindexService;

    public ElasticsearchReindexResource(ElasticsearchReindexService reindexService) {
        this.reindexService = reindexService;
    }

    @PostMapping("/reindex")
    @Operation(
        summary = "Bắt đầu reindex (chạy nền)",
        description = "Xóa và tạo lại index rồi đưa toàn bộ dữ liệu MySQL vào Elasticsearch. Trả về 202 ngay; " +
            "theo dõi tiến độ bằng GET /api/admin/elasticsearch/reindex. Listing (~1,86 triệu dòng) mất vài phút. " +
            "Trong lúc chạy, tìm kiếm trên entity đang reindex chỉ trả một phần kết quả."
    )
    public ResponseEntity<ReindexStatus> startReindex(
        @Parameter(description = "Entity cần reindex, bỏ trống = tất cả: category, location, tag, blogpost, user, listing") @RequestParam(
            name = "entities",
            required = false
        ) List<String> entities
    ) {
        LOG.info("REST request to reindex Elasticsearch: {}", entities);
        try {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(reindexService.start(entities));
        } catch (IllegalArgumentException e) {
            throw new BadRequestAlertException(e.getMessage(), "elasticsearch", "invalidentity");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(reindexService.getStatus());
        }
    }

    @GetMapping("/reindex")
    @Operation(summary = "Trạng thái và tiến độ reindex")
    public ReindexStatus getReindexStatus() {
        return reindexService.getStatus();
    }

    @GetMapping("/indices")
    @Operation(
        summary = "Kiểm tra index đã đủ chưa",
        description = "So số dòng MySQL (dbCount) với số tài liệu Elasticsearch (esCount) của từng entity. " +
            "complete=false thì cần reindex entity đó; esCount=-1 là index chưa tồn tại."
    )
    public List<IndexCheck> checkIndices() {
        return reindexService.checkIndices();
    }
}
