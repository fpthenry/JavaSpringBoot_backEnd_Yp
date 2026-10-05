package com.mycompany.myapp.service;

import com.mycompany.myapp.service.dto.BlogCategoryDTO;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.mycompany.myapp.domain.BlogCategory}.
 */
public interface BlogCategoryService {
    /**
     * Save a blogCategory.
     *
     * @param blogCategoryDTO the entity to save.
     * @return the persisted entity.
     */
    BlogCategoryDTO save(BlogCategoryDTO blogCategoryDTO);

    /**
     * Updates a blogCategory.
     *
     * @param blogCategoryDTO the entity to update.
     * @return the persisted entity.
     */
    BlogCategoryDTO update(BlogCategoryDTO blogCategoryDTO);

    /**
     * Partially updates a blogCategory.
     *
     * @param blogCategoryDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<BlogCategoryDTO> partialUpdate(BlogCategoryDTO blogCategoryDTO);

    /**
     * Get all the blogCategories with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<BlogCategoryDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Get the "id" blogCategory.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<BlogCategoryDTO> findOne(Long id);

    /**
     * Delete the "id" blogCategory.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);

    /**
     * Search for the blogCategory corresponding to the query.
     *
     * @param query the query of the search.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<BlogCategoryDTO> search(String query, Pageable pageable);
}
