package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.GalleryImage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the GalleryImage entity.
 */
@Repository
public interface GalleryImageRepository extends JpaRepository<GalleryImage, Long>, JpaSpecificationExecutor<GalleryImage> {
    default Optional<GalleryImage> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<GalleryImage> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<GalleryImage> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select galleryImage from GalleryImage galleryImage left join fetch galleryImage.gallery",
        countQuery = "select count(galleryImage) from GalleryImage galleryImage"
    )
    Page<GalleryImage> findAllWithToOneRelationships(Pageable pageable);

    @Query("select galleryImage from GalleryImage galleryImage left join fetch galleryImage.gallery")
    List<GalleryImage> findAllWithToOneRelationships();

    @Query("select galleryImage from GalleryImage galleryImage left join fetch galleryImage.gallery where galleryImage.id =:id")
    Optional<GalleryImage> findOneWithToOneRelationships(@Param("id") Long id);
}
