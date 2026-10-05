package com.mycompany.myapp.service.dto.publicapi;

import java.io.Serializable;
import java.util.List;

/**
 * Một vị trí banner trả cho FE qua API công khai, kèm các ảnh đang chạy theo thứ tự hiển thị.
 */
public record PublicGalleryDTO(String code, String name, String description, List<Image> images) implements Serializable {
    /**
     * Một ảnh banner.
     *
     * @param imageUrl     URL ảnh tuyệt đối: link ảnh có sẵn, hoặc URL endpoint media nếu ảnh được upload.
     * @param linkUrl      bấm vào ảnh thì mở link này (có thể null).
     * @param openInNewTab mở link ở tab mới.
     */
    public record Image(
        Long id,
        String title,
        String imageUrl,
        String linkUrl,
        String altText,
        boolean openInNewTab,
        int displayOrder
    ) implements Serializable {}
}
