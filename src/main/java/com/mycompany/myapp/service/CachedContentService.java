package com.mycompany.myapp.service;

import com.mycompany.myapp.service.dto.CachedContentDTO;
import java.util.List;
import java.util.Optional;

/**
 * Service Interface for managing {@link com.mycompany.myapp.domain.CachedContent}.
 */
public interface CachedContentService {
    /**
     * Save a cachedContent.
     *
     * @param cachedContentDTO the entity to save.
     * @return the persisted entity.
     */
    CachedContentDTO save(CachedContentDTO cachedContentDTO);

    /**
     * Updates a cachedContent.
     *
     * @param cachedContentDTO the entity to update.
     * @return the persisted entity.
     */
    CachedContentDTO update(CachedContentDTO cachedContentDTO);

    /**
     * Partially updates a cachedContent.
     *
     * @param cachedContentDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<CachedContentDTO> partialUpdate(CachedContentDTO cachedContentDTO);

    /**
     * Get all the cachedContents.
     *
     * @return the list of entities.
     */
    List<CachedContentDTO> findAll();

    /**
     * Get the "id" cachedContent.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<CachedContentDTO> findOne(Long id);

    /**
     * Delete the "id" cachedContent.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);

    /**
     * Search for the cachedContent corresponding to the query.
     *
     * @param query the query of the search.
     * @return the list of entities.
     */
    List<CachedContentDTO> search(String query);
}
