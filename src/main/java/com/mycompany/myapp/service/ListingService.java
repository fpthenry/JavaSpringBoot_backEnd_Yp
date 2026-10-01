package com.mycompany.myapp.service;

import com.mycompany.myapp.service.dto.ListingDTO;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.mycompany.myapp.domain.Listing}.
 */
public interface ListingService {
    /**
     * Save a listing.
     *
     * @param listingDTO the entity to save.
     * @return the persisted entity.
     */
    ListingDTO save(ListingDTO listingDTO);

    /**
     * Updates a listing.
     *
     * @param listingDTO the entity to update.
     * @return the persisted entity.
     */
    ListingDTO update(ListingDTO listingDTO);

    /**
     * Partially updates a listing.
     *
     * @param listingDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<ListingDTO> partialUpdate(ListingDTO listingDTO);

    /**
     * Get all the listings with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<ListingDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Get the "id" listing.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<ListingDTO> findOne(Long id);

    /**
     * Delete the "id" listing.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);

    /**
     * Search for the listing corresponding to the query.
     *
     * @param query the query of the search.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<ListingDTO> search(String query, Pageable pageable);
}
