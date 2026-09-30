package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.ListingImage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ListingImage entity.
 */
@Repository
public interface ListingImageRepository extends JpaRepository<ListingImage, Long>, JpaSpecificationExecutor<ListingImage> {
    default Optional<ListingImage> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<ListingImage> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<ListingImage> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select listingImage from ListingImage listingImage left join fetch listingImage.listing left join fetch listingImage.gallery",
        countQuery = "select count(listingImage) from ListingImage listingImage"
    )
    Page<ListingImage> findAllWithToOneRelationships(Pageable pageable);

    @Query("select listingImage from ListingImage listingImage left join fetch listingImage.listing left join fetch listingImage.gallery")
    List<ListingImage> findAllWithToOneRelationships();

    @Query(
        "select listingImage from ListingImage listingImage left join fetch listingImage.listing left join fetch listingImage.gallery where listingImage.id =:id"
    )
    Optional<ListingImage> findOneWithToOneRelationships(@Param("id") Long id);
}
