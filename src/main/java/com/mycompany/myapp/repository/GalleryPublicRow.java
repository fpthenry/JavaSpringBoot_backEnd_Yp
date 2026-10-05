package com.mycompany.myapp.repository;

/**
 * Một ảnh banner đang chạy, đọc cho API công khai. Không chứa dữ liệu ảnh nhị phân
 * ({@code uploaded = true} nghĩa là ảnh lưu trong database, tải qua endpoint media).
 */
public record GalleryPublicRow(
    Long id,
    String galleryCode,
    String title,
    String imageUrl,
    boolean uploaded,
    String linkUrl,
    String altText,
    Boolean openInNewTab,
    Integer displayOrder
) {}
