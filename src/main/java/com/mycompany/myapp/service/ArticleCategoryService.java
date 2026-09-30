package com.mycompany.myapp.service;

import com.mycompany.myapp.service.dto.ArticleCategoryDTO;
import java.util.List;
import java.util.Optional;

/**
 * Service Interface for managing {@link com.mycompany.myapp.domain.ArticleCategory}.
 */
public interface ArticleCategoryService {
    /**
     * Save a articleCategory.
     *
     * @param articleCategoryDTO the entity to save.
     * @return the persisted entity.
     */
    ArticleCategoryDTO save(ArticleCategoryDTO articleCategoryDTO);

    /**
     * Updates a articleCategory.
     *
     * @param articleCategoryDTO the entity to update.
     * @return the persisted entity.
     */
    ArticleCategoryDTO update(ArticleCategoryDTO articleCategoryDTO);

    /**
     * Partially updates a articleCategory.
     *
     * @param articleCategoryDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<ArticleCategoryDTO> partialUpdate(ArticleCategoryDTO articleCategoryDTO);

    /**
     * Get all the articleCategories.
     *
     * @return the list of entities.
     */
    List<ArticleCategoryDTO> findAll();

    /**
     * Get all the articleCategories with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    List<ArticleCategoryDTO> findAllWithEagerRelationships();

    /**
     * Get the "id" articleCategory.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<ArticleCategoryDTO> findOne(Long id);

    /**
     * Delete the "id" articleCategory.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);

    /**
     * Search for the articleCategory corresponding to the query.
     *
     * @param query the query of the search.
     * @return the list of entities.
     */
    List<ArticleCategoryDTO> search(String query);
}
