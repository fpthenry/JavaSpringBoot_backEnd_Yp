package com.mycompany.myapp.service;

import com.mycompany.myapp.service.dto.RedirectRuleDTO;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.mycompany.myapp.domain.RedirectRule}.
 */
public interface RedirectRuleService {
    /**
     * Save a redirectRule.
     *
     * @param redirectRuleDTO the entity to save.
     * @return the persisted entity.
     */
    RedirectRuleDTO save(RedirectRuleDTO redirectRuleDTO);

    /**
     * Updates a redirectRule.
     *
     * @param redirectRuleDTO the entity to update.
     * @return the persisted entity.
     */
    RedirectRuleDTO update(RedirectRuleDTO redirectRuleDTO);

    /**
     * Partially updates a redirectRule.
     *
     * @param redirectRuleDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<RedirectRuleDTO> partialUpdate(RedirectRuleDTO redirectRuleDTO);

    /**
     * Get the "id" redirectRule.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<RedirectRuleDTO> findOne(Long id);

    /**
     * Delete the "id" redirectRule.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);

    /**
     * Search for the redirectRule corresponding to the query.
     *
     * @param query the query of the search.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<RedirectRuleDTO> search(String query, Pageable pageable);
}
