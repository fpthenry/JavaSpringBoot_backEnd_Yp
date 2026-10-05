package com.mycompany.myapp.service.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * Một vị trí banner kèm mọi ảnh của nó, cho API quản trị "tất cả banner kèm ảnh".
 * Gồm cả vị trí và ảnh đang tắt hoặc hết hạn; ảnh upload trả URL thay cho base64.
 */
public record GalleryWithImagesDTO(
    Long id,
    String name,
    String code,
    String description,
    Boolean active,
    Long wpId,
    int imageCount,
    List<Image> images
) implements Serializable {
    /**
     * @param uploaded    true: ảnh upload (lưu trong database); false: dùng link ảnh có sẵn.
     * @param previewUrl  URL xem ảnh: endpoint media nếu upload, ngược lại là imageUrl (có thể null nếu chưa có ảnh).
     * @param imageUrl    link ảnh có sẵn đã nhập (có thể null).
     * @param showingNow  true nếu ảnh đang hiện trên FE: ảnh và vị trí đều bật, trong khoảng startAt-endAt, có ảnh.
     */
    public record Image(
        Long id,
        String title,
        boolean uploaded,
        String imageContentType,
        String previewUrl,
        String imageUrl,
        String linkUrl,
        String altText,
        Integer displayOrder,
        Boolean active,
        Instant startAt,
        Instant endAt,
        Boolean openInNewTab,
        boolean showingNow
    ) implements Serializable {}
}
