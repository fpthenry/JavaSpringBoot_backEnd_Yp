package com.mycompany.myapp.service;

import com.mycompany.myapp.service.dto.StaticPageDTO;
import java.util.List;
import java.util.Optional;

/**
 * Service Interface for managing {@link com.mycompany.myapp.domain.StaticPage}.
 */
public interface StaticPageService {
    /**
     * Save a staticPage.
     *
     * @param staticPageDTO the entity to save.
     * @return the persisted entity.
     */
    StaticPageDTO save(StaticPageDTO staticPageDTO);

    /**
     * Updates a staticPage.
     *
     * @param staticPageDTO the entity to update.
     * @return the persisted entity.
     */
    StaticPageDTO update(StaticPageDTO staticPageDTO);

    /**
     * Partially updates a staticPage.
     *
     * @param staticPageDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<StaticPageDTO> partialUpdate(StaticPageDTO staticPageDTO);

    /**
     * Get all the staticPages.
     *
     * @return the list of entities.
     */
    List<StaticPageDTO> findAll();

    /**
     * Get all the staticPages with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    List<StaticPageDTO> findAllWithEagerRelationships();

    /**
     * Get the "id" staticPage.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<StaticPageDTO> findOne(Long id);

    /**
     * Delete the "id" staticPage.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);

    /**
     * Search for the staticPage corresponding to the query.
     *
     * @param query the query of the search.
     * @return the list of entities.
     */
    List<StaticPageDTO> search(String query);
}
