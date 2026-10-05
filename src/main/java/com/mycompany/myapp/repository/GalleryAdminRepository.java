package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Gallery;
import java.util.List;
import org.springframework.data.jpa.repository.Query;

/**
 * Truy vấn chỉ đọc cho API quản trị "tất cả banner kèm ảnh". Viết tay, tách khỏi repository JHipster sinh ra.
 * Danh sách ảnh không tải dữ liệu nhị phân.
 */
public interface GalleryAdminRepository extends org.springframework.data.repository.Repository<Gallery, Long> {
    /** Mọi vị trí banner, kể cả đang tắt. */
    @Query("select g from Gallery g order by g.code, g.id")
    List<Gallery> findAllGalleries();

    /** Mọi ảnh, kể cả đang tắt và hết hạn. */
    @Query(
        "select new com.mycompany.myapp.repository.GalleryImageAdminRow(i.id, i.gallery.id, i.title, " +
            "case when i.image is null then false else true end, i.imageContentType, i.imageUrl, i.linkUrl, i.altText, " +
            "i.displayOrder, i.active, i.startAt, i.endAt, i.openInNewTab) " +
            "from GalleryImage i order by i.displayOrder, i.id"
    )
    List<GalleryImageAdminRow> findAllImages();
}
