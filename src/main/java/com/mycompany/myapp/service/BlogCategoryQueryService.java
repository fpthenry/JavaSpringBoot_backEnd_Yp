package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.*; // for static metamodels
import com.mycompany.myapp.domain.BlogCategory;
import com.mycompany.myapp.repository.BlogCategoryRepository;
import com.mycompany.myapp.repository.search.BlogCategorySearchRepository;
import com.mycompany.myapp.service.criteria.BlogCategoryCriteria;
import com.mycompany.myapp.service.dto.BlogCategoryDTO;
import com.mycompany.myapp.service.mapper.BlogCategoryMapper;
import jakarta.persistence.criteria.JoinType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link BlogCategory} entities in the database.
 * The main input is a {@link BlogCategoryCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link BlogCategoryDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class BlogCategoryQueryService extends QueryService<BlogCategory> {

    private static final Logger LOG = LoggerFactory.getLogger(BlogCategoryQueryService.class);

    private final BlogCategoryRepository blogCategoryRepository;

    private final BlogCategoryMapper blogCategoryMapper;

    private final BlogCategorySearchRepository blogCategorySearchRepository;

    public BlogCategoryQueryService(
        BlogCategoryRepository blogCategoryRepository,
        BlogCategoryMapper blogCategoryMapper,
        BlogCategorySearchRepository blogCategorySearchRepository
    ) {
        this.blogCategoryRepository = blogCategoryRepository;
        this.blogCategoryMapper = blogCategoryMapper;
        this.blogCategorySearchRepository = blogCategorySearchRepository;
    }

    /**
     * Return a {@link Page} of {@link BlogCategoryDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<BlogCategoryDTO> findByCriteria(BlogCategoryCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<BlogCategory> specification = createSpecification(criteria);
        return blogCategoryRepository.findAll(specification, page).map(blogCategoryMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(BlogCategoryCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<BlogCategory> specification = createSpecification(criteria);
        return blogCategoryRepository.count(specification);
    }

    /**
     * Function to convert {@link BlogCategoryCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<BlogCategory> createSpecification(BlogCategoryCriteria criteria) {
        Specification<BlogCategory> specification = Specification.unrestricted();
        specification = specification.and((root, query, builder) -> {
            if (Long.class != query.getResultType()) {
                root.fetch(BlogCategory_.parent, JoinType.LEFT);
            }
            return null;
        });
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildRangeSpecification(criteria.getId(), BlogCategory_.id),
                    buildRangeSpecification(criteria.getWpTermId(), BlogCategory_.wpTermId),
                    buildStringSpecification(criteria.getName(), BlogCategory_.name),
                    buildStringSpecification(criteria.getSlug(), BlogCategory_.slug),
                    buildRangeSpecification(criteria.getPostCount(), BlogCategory_.postCount),
                    buildSpecification(criteria.getParentId(), root ->
                        root.join(BlogCategory_.parent, JoinType.LEFT).get(BlogCategory_.id)
                    ),
                    buildSpecification(criteria.getBlogPostId(), root ->
                        root.join(BlogCategory_.blogPosts, JoinType.LEFT).get(BlogPost_.id)
                    )
                )
            );
        }
        return specification;
    }
}
