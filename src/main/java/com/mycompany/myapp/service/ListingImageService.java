package com.mycompany.myapp.service;

import com.mycompany.myapp.service.dto.ListingImageDTO;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.mycompany.myapp.domain.ListingImage}.
 */
public interface ListingImageService {
    /**
     * Save a listingImage.
     *
     * @param listingImageDTO the entity to save.
     * @return the persisted entity.
     */
    ListingImageDTO save(ListingImageDTO listingImageDTO);

    /**
     * Updates a listingImage.
     *
     * @param listingImageDTO the entity to update.
     * @return the persisted entity.
     */
    ListingImageDTO update(ListingImageDTO listingImageDTO);

    /**
     * Partially updates a listingImage.
     *
     * @param listingImageDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<ListingImageDTO> partialUpdate(ListingImageDTO listingImageDTO);

    /**
     * Get all the listingImages with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<ListingImageDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Get the "id" listingImage.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<ListingImageDTO> findOne(Long id);

    /**
     * Delete the "id" listingImage.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);

    /**
     * Search for the listingImage corresponding to the query.
     *
     * @param query the query of the search.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<ListingImageDTO> search(String query, Pageable pageable);
}
