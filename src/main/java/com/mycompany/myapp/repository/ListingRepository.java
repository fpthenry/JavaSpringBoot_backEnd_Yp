package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Listing;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Listing entity.
 *
 * When extending this class, extend ListingRepositoryWithBagRelationships too.
 * For more information refer to https://github.com/jhipster/generator-jhipster/issues/17990.
 */
@Repository
public interface ListingRepository
    extends ListingRepositoryWithBagRelationships, JpaRepository<Listing, Long>, JpaSpecificationExecutor<Listing>
{
    @Query("select listing from Listing listing where listing.author.login = ?#{authentication.name}")
    List<Listing> findByAuthorIsCurrentUser();

    default Optional<Listing> findOneWithEagerRelationships(Long id) {
        return this.fetchBagRelationships(this.findOneWithToOneRelationships(id));
    }

    default List<Listing> findAllWithEagerRelationships() {
        return this.fetchBagRelationships(this.findAllWithToOneRelationships());
    }

    default Page<Listing> findAllWithEagerRelationships(Pageable pageable) {
        return this.fetchBagRelationships(this.findAllWithToOneRelationships(pageable));
    }

    @Query(
        value = "select listing from Listing listing left join fetch listing.author",
        countQuery = "select count(listing) from Listing listing"
    )
    Page<Listing> findAllWithToOneRelationships(Pageable pageable);

    @Query("select listing from Listing listing left join fetch listing.author")
    List<Listing> findAllWithToOneRelationships();

    @Query("select listing from Listing listing left join fetch listing.author where listing.id =:id")
    Optional<Listing> findOneWithToOneRelationships(@Param("id") Long id);
}
