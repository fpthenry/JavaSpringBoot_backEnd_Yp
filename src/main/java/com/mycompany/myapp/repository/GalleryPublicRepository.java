package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Gallery;
import com.mycompany.myapp.domain.GalleryImage;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Truy vấn chỉ đọc cho API công khai gallery/banner. Viết tay, tách khỏi {@link GalleryImageRepository}
 * do JHipster sinh, để sinh lại code không mất.
 */
public interface GalleryPublicRepository extends org.springframework.data.repository.Repository<GalleryImage, Long> {
    String CURRENT_IMAGE =
        "i.active = true and g.active = true " +
        "and (i.startAt is null or i.startAt <= :now) and (i.endAt is null or i.endAt > :now) " +
        "and (i.image is not null or i.imageUrl is not null)";

    String ROW =
        "select new com.mycompany.myapp.repository.GalleryPublicRow(i.id, g.code, i.title, i.imageUrl, " +
        "case when i.image is null then false else true end, i.linkUrl, i.altText, i.openInNewTab, i.displayOrder) " +
        "from GalleryImage i join i.gallery g ";

    /** Ảnh đang chạy của các vị trí có mã trong {@code codes}, theo thứ tự hiển thị. */
    @Query(ROW + "where g.code in :codes and " + CURRENT_IMAGE + " order by g.code, i.displayOrder, i.id")
    List<GalleryPublicRow> findCurrentImages(@Param("codes") Collection<String> codes, @Param("now") Instant now);

    /** Ảnh đang chạy của mọi vị trí đang bật. */
    @Query(ROW + "where " + CURRENT_IMAGE + " order by g.code, i.displayOrder, i.id")
    List<GalleryPublicRow> findAllCurrentImages(@Param("now") Instant now);

    @Query("select g from Gallery g where g.active = true and g.code in :codes order by g.code")
    List<Gallery> findActiveGalleries(@Param("codes") Collection<String> codes);

    @Query("select g from Gallery g where g.active = true order by g.code")
    List<Gallery> findAllActiveGalleries();

    /** Ảnh upload (có dữ liệu nhị phân) theo id, để phục vụ file ảnh. */
    @Query("select i from GalleryImage i where i.id = :id and i.image is not null")
    Optional<GalleryImage> findUploadedImage(@Param("id") Long id);
}
