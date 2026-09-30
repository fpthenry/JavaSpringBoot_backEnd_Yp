package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.*; // for static metamodels
import com.mycompany.myapp.domain.RedirectRule;
import com.mycompany.myapp.repository.RedirectRuleRepository;
import com.mycompany.myapp.repository.search.RedirectRuleSearchRepository;
import com.mycompany.myapp.service.criteria.RedirectRuleCriteria;
import com.mycompany.myapp.service.dto.RedirectRuleDTO;
import com.mycompany.myapp.service.mapper.RedirectRuleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link RedirectRule} entities in the database.
 * The main input is a {@link RedirectRuleCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link RedirectRuleDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class RedirectRuleQueryService extends QueryService<RedirectRule> {

    private static final Logger LOG = LoggerFactory.getLogger(RedirectRuleQueryService.class);

    private final RedirectRuleRepository redirectRuleRepository;

    private final RedirectRuleMapper redirectRuleMapper;

    private final RedirectRuleSearchRepository redirectRuleSearchRepository;

    public RedirectRuleQueryService(
        RedirectRuleRepository redirectRuleRepository,
        RedirectRuleMapper redirectRuleMapper,
        RedirectRuleSearchRepository redirectRuleSearchRepository
    ) {
        this.redirectRuleRepository = redirectRuleRepository;
        this.redirectRuleMapper = redirectRuleMapper;
        this.redirectRuleSearchRepository = redirectRuleSearchRepository;
    }

    /**
     * Return a {@link Page} of {@link RedirectRuleDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<RedirectRuleDTO> findByCriteria(RedirectRuleCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<RedirectRule> specification = createSpecification(criteria);
        return redirectRuleRepository.findAll(specification, page).map(redirectRuleMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(RedirectRuleCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<RedirectRule> specification = createSpecification(criteria);
        return redirectRuleRepository.count(specification);
    }

    /**
     * Function to convert {@link RedirectRuleCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<RedirectRule> createSpecification(RedirectRuleCriteria criteria) {
        Specification<RedirectRule> specification = Specification.unrestricted();
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildRangeSpecification(criteria.getId(), RedirectRule_.id),
                    buildRangeSpecification(criteria.getSourceId(), RedirectRule_.sourceId),
                    buildStringSpecification(criteria.getSourceSlug(), RedirectRule_.sourceSlug),
                    buildRangeSpecification(criteria.getDestinationId(), RedirectRule_.destinationId),
                    buildStringSpecification(criteria.getDestinationSlug(), RedirectRule_.destinationSlug),
                    buildStringSpecification(criteria.getObjectType(), RedirectRule_.objectType)
                )
            );
        }
        return specification;
    }
}
