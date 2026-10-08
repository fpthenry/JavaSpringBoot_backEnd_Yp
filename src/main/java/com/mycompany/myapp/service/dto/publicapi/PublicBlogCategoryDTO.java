package com.mycompany.myapp.service.dto.publicapi;

import java.io.Serializable;
import java.util.List;

/**
 * Một danh mục bài viết trong cây, trả cho FE qua API công khai.
 *
 * @param postCount      số bài đã xuất bản gắn trực tiếp vào danh mục này.
 * @param totalPostCount số bài đã xuất bản của danh mục này và mọi danh mục con (mỗi bài đếm một lần).
 * @param children       danh mục con, sắp theo tên.
 */
public record PublicBlogCategoryDTO(
    Long id,
    String name,
    String slug,
    String description,
    Long parentId,
    long postCount,
    long totalPostCount,
    List<PublicBlogCategoryDTO> children
) implements Serializable {}
