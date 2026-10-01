package com.mycompany.myapp.service;

import com.mycompany.myapp.service.dto.BlogPostDTO;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.mycompany.myapp.domain.BlogPost}.
 */
public interface BlogPostService {
    /**
     * Save a blogPost.
     *
     * @param blogPostDTO the entity to save.
     * @return the persisted entity.
     */
    BlogPostDTO save(BlogPostDTO blogPostDTO);

    /**
     * Updates a blogPost.
     *
     * @param blogPostDTO the entity to update.
     * @return the persisted entity.
     */
    BlogPostDTO update(BlogPostDTO blogPostDTO);

    /**
     * Partially updates a blogPost.
     *
     * @param blogPostDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<BlogPostDTO> partialUpdate(BlogPostDTO blogPostDTO);

    /**
     * Get the "id" blogPost.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<BlogPostDTO> findOne(Long id);

    /**
     * Delete the "id" blogPost.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);

    /**
     * Search for the blogPost corresponding to the query.
     *
     * @param query the query of the search.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<BlogPostDTO> search(String query, Pageable pageable);
}
