package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Gallery;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Gallery entity.
 */
@Repository
public interface GalleryRepository extends JpaRepository<Gallery, Long> {
    @Query("select gallery from Gallery gallery where gallery.author.login = ?#{authentication.name}")
    List<Gallery> findByAuthorIsCurrentUser();

    default Optional<Gallery> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Gallery> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Gallery> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select gallery from Gallery gallery left join fetch gallery.author left join fetch gallery.listing",
        countQuery = "select count(gallery) from Gallery gallery"
    )
    Page<Gallery> findAllWithToOneRelationships(Pageable pageable);

    @Query("select gallery from Gallery gallery left join fetch gallery.author left join fetch gallery.listing")
    List<Gallery> findAllWithToOneRelationships();

    @Query("select gallery from Gallery gallery left join fetch gallery.author left join fetch gallery.listing where gallery.id =:id")
    Optional<Gallery> findOneWithToOneRelationships(@Param("id") Long id);
}
