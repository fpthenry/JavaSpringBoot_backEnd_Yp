package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.service.GalleryAdminService;
import com.mycompany.myapp.service.dto.GalleryWithImagesDTO;
import com.mycompany.myapp.web.rest.publicapi.PublicGalleryResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quản trị banner: một API lấy tất cả vị trí kèm mọi ảnh (JWT). Viết tay, bổ sung cho {@link GalleryResource}
 * và {@link GalleryImageResource} do JHipster sinh (có phân trang, ảnh dạng base64).
 * Đường dẫn {@code /with-images} được Spring ưu tiên hơn {@code /{id}}.
 */
@RestController
@RequestMapping("/api/galleries")
@Tag(name = "gallery-admin-resource", description = "Quản trị banner: xem tất cả vị trí kèm mọi ảnh (JWT)")
public class GalleryAdminResource {

    private static final Logger LOG = LoggerFactory.getLogger(GalleryAdminResource.class);

    private final GalleryAdminService galleryAdminService;

    public GalleryAdminResource(GalleryAdminService galleryAdminService) {
        this.galleryAdminService = galleryAdminService;
    }

    @GetMapping("/with-images")
    @Operation(
        summary = "Tất cả banner kèm tất cả ảnh",
        description = "Không tham số, không phân trang. Trả mọi vị trí banner (kể cả đang tắt), mỗi vị trí kèm mọi ảnh " +
            "(kể cả đang tắt, hết hạn) với đầy đủ thông tin. Ảnh upload trả previewUrl (mở được trên trình duyệt) thay cho base64. " +
            "showingNow = ảnh đang hiện trên FE."
    )
    public List<GalleryWithImagesDTO> getAllGalleriesWithImages() {
        LOG.debug("REST request to get all Galleries with all images");
        return galleryAdminService.findAllWithImages(PublicGalleryResource.uploadedImageUrl());
    }
}
