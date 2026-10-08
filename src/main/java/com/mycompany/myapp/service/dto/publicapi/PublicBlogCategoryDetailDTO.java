package com.mycompany.myapp.service.dto.publicapi;

import java.io.Serializable;
import java.util.List;

/**
 * Một danh mục bài viết kèm đường dẫn từ gốc (breadcrumb), cho trang danh mục của FE.
 *
 * @param category   danh mục, kèm cây con và số bài.
 * @param breadcrumb các danh mục từ gốc tới danh mục cha (không gồm chính nó); rỗng với danh mục gốc.
 */
public record PublicBlogCategoryDetailDTO(
    PublicBlogCategoryDTO category,
    List<PublicBlogPostDTO.Term> breadcrumb
) implements Serializable {}
