package com.mycompany.myapp.repository;

import java.time.Instant;

/**
 * Một ảnh banner cho API quản trị "tất cả banner kèm ảnh". Không chứa dữ liệu ảnh nhị phân
 * ({@code uploaded = true} nghĩa là ảnh lưu trong database, xem qua endpoint media).
 */
public record GalleryImageAdminRow(
    Long id,
    Long galleryId,
    String title,
    boolean uploaded,
    String imageContentType,
    String imageUrl,
    String linkUrl,
    String altText,
    Integer displayOrder,
    Boolean active,
    Instant startAt,
    Instant endAt,
    Boolean openInNewTab
) {}
