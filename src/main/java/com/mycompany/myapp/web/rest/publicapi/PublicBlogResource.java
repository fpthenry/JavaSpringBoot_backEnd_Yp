package com.mycompany.myapp.web.rest.publicapi;

import com.mycompany.myapp.service.PublicBlogService;
import com.mycompany.myapp.service.dto.publicapi.PublicBlogCategoryDTO;
import com.mycompany.myapp.service.dto.publicapi.PublicBlogCategoryDetailDTO;
import com.mycompany.myapp.service.dto.publicapi.PublicBlogPostDTO;
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
 * API công khai bài viết (tin tức, sự kiện) cho FE (Next.js). Cần header {@code X-API-Key}. Chỉ trả bài đã xuất bản.
 */
@RestController
@RequestMapping("/api/public/v1")
@Tag(name = "public-blog", description = "API công khai: bài viết và cây danh mục bài viết (cần header X-API-Key)")
public class PublicBlogResource {

    /** FE có thể cache trong thời gian này; bài mới đăng tối đa sau chừng ấy mới hiện. */
    private static final CacheControl JSON_CACHE = CacheControl.maxAge(Duration.ofMinutes(1)).cachePublic();

    private final PublicBlogService blogService;

    public PublicBlogResource(PublicBlogService blogService) {
        this.blogService = blogService;
    }

    @GetMapping("/blog-categories/tree")
    @Operation(
        summary = "Cây danh mục bài viết",
        description = "Danh mục gốc, mỗi danh mục có children (sắp theo tên). postCount: số bài đã xuất bản gắn trực tiếp; " +
            "totalPostCount: tính cả danh mục con, mỗi bài đếm một lần. Dùng cho menu, sidebar."
    )
    public ResponseEntity<List<PublicBlogCategoryDTO>> getCategoryTree(
        @Parameter(description = "true: bỏ danh mục không có bài đã xuất bản nào (tính cả danh mục con)") @RequestParam(
            name = "hideEmpty",
            defaultValue = "false"
        ) boolean hideEmpty
    ) {
        return ResponseEntity.ok().cacheControl(JSON_CACHE).body(blogService.categoryTree(hideEmpty));
    }

    @GetMapping("/blog-categories/{slugOrId}")
    @Operation(
        summary = "Một danh mục bài viết, kèm danh mục con và breadcrumb",
        description = "Tìm theo slug (ví dụ tin-tuc) hoặc id. breadcrumb: các danh mục cha từ gốc. 404 nếu không có."
    )
    public ResponseEntity<PublicBlogCategoryDetailDTO> getCategory(@PathVariable("slugOrId") String slugOrId) {
        return blogService
            .category(slugOrId)
            .map(category -> ResponseEntity.ok().cacheControl(JSON_CACHE).body(category))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/blog-posts")
    @Operation(
        summary = "Danh sách bài viết đã xuất bản, lọc theo danh mục, thẻ, từ khóa",
        description = "category: slug hoặc id danh mục, lấy cả bài của danh mục con (404 nếu không có danh mục). " +
            "tag: slug hoặc id thẻ. q: từ khóa (có dấu khớp đúng dấu, không dấu khớp mọi dấu). " +
            "Không có sort: bài mới đăng trước; có q thì theo độ liên quan. sort nhận publishedAt, viewCount, id. " +
            "size tối đa 100. Danh sách không có content (lấy ở API chi tiết)."
    )
    public ResponseEntity<PublicPageDTO<PublicBlogPostDTO>> getPosts(
        @Parameter(description = "Slug hoặc id danh mục, ví dụ tin-tuc") @RequestParam(name = "category", required = false) String category,
        @Parameter(description = "Slug hoặc id thẻ") @RequestParam(name = "tag", required = false) String tag,
        @Parameter(description = "Từ khóa tìm trong tiêu đề, tóm tắt, nội dung, tên danh mục, thẻ") @RequestParam(
            name = "q",
            required = false
        ) String q,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok()
            .cacheControl(JSON_CACHE)
            .body(blogService.posts(category, tag, q, pageable));
    }

    @GetMapping("/blog-posts/{slug}")
    @Operation(
        summary = "Chi tiết bài viết đã xuất bản theo slug",
        description = "Có content (HTML, đã bỏ shortcode WPBakery), danh mục, thẻ. 404 nếu không có hoặc là bài nháp."
    )
    public ResponseEntity<PublicBlogPostDTO> getPost(@PathVariable("slug") String slug) {
        return blogService
            .post(slug)
            .map(post -> ResponseEntity.ok().cacheControl(JSON_CACHE).body(post))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
