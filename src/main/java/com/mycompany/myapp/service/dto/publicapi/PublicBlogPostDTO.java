package com.mycompany.myapp.service.dto.publicapi;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * Bài viết trả cho FE qua API công khai.
 *
 * @param excerpt   tóm tắt dạng chữ thuần (đã bỏ HTML); bài không có tóm tắt thì lấy đầu nội dung.
 * @param content   nội dung HTML (đã bỏ shortcode WPBakery {@code [vc_...]}); chỉ có ở API chi tiết, danh sách trả null.
 * @param thumbnail URL ảnh đại diện (có thể null).
 */
public record PublicBlogPostDTO(
    Long id,
    String title,
    String slug,
    String excerpt,
    String content,
    String thumbnail,
    String authorName,
    Integer viewCount,
    Instant publishedAt,
    Instant updatedAt,
    List<Term> categories,
    List<Term> tags
) implements Serializable {
    /** Danh mục hoặc thẻ gắn với bài. */
    public record Term(Long id, String name, String slug) implements Serializable {}
}
