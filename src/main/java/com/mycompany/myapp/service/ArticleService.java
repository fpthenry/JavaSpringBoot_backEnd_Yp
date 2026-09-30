package com.mycompany.myapp.service;

import com.mycompany.myapp.service.dto.ArticleDTO;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.mycompany.myapp.domain.Article}.
 */
public interface ArticleService {
    /**
     * Save a article.
     *
     * @param articleDTO the entity to save.
     * @return the persisted entity.
     */
    ArticleDTO save(ArticleDTO articleDTO);

    /**
     * Updates a article.
     *
     * @param articleDTO the entity to update.
     * @return the persisted entity.
     */
    ArticleDTO update(ArticleDTO articleDTO);

    /**
     * Partially updates a article.
     *
     * @param articleDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<ArticleDTO> partialUpdate(ArticleDTO articleDTO);

    /**
     * Get all the articles with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<ArticleDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Get the "id" article.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<ArticleDTO> findOne(Long id);

    /**
     * Delete the "id" article.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);

    /**
     * Search for the article corresponding to the query.
     *
     * @param query the query of the search.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<ArticleDTO> search(String query, Pageable pageable);
}
