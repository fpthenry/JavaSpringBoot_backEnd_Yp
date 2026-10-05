package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.*; // for static metamodels
import com.mycompany.myapp.domain.BlogPost;
import com.mycompany.myapp.repository.BlogPostRepository;
import com.mycompany.myapp.repository.search.BlogPostSearchRepository;
import com.mycompany.myapp.service.criteria.BlogPostCriteria;
import com.mycompany.myapp.service.dto.BlogPostDTO;
import com.mycompany.myapp.service.mapper.BlogPostMapper;
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
 * Service for executing complex queries for {@link BlogPost} entities in the database.
 * The main input is a {@link BlogPostCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link BlogPostDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class BlogPostQueryService extends QueryService<BlogPost> {

    private static final Logger LOG = LoggerFactory.getLogger(BlogPostQueryService.class);

    private final BlogPostRepository blogPostRepository;

    private final BlogPostMapper blogPostMapper;

    private final BlogPostSearchRepository blogPostSearchRepository;

    public BlogPostQueryService(
        BlogPostRepository blogPostRepository,
        BlogPostMapper blogPostMapper,
        BlogPostSearchRepository blogPostSearchRepository
    ) {
        this.blogPostRepository = blogPostRepository;
        this.blogPostMapper = blogPostMapper;
        this.blogPostSearchRepository = blogPostSearchRepository;
    }

    /**
     * Return a {@link Page} of {@link BlogPostDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<BlogPostDTO> findByCriteria(BlogPostCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<BlogPost> specification = createSpecification(criteria);
        return blogPostRepository.fetchBagRelationships(blogPostRepository.findAll(specification, page)).map(blogPostMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(BlogPostCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<BlogPost> specification = createSpecification(criteria);
        return blogPostRepository.count(specification);
    }

    /**
     * Function to convert {@link BlogPostCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<BlogPost> createSpecification(BlogPostCriteria criteria) {
        Specification<BlogPost> specification = Specification.unrestricted();
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildRangeSpecification(criteria.getId(), BlogPost_.id),
                    buildRangeSpecification(criteria.getWpId(), BlogPost_.wpId),
                    buildStringSpecification(criteria.getTitle(), BlogPost_.title),
                    buildStringSpecification(criteria.getSlug(), BlogPost_.slug),
                    buildStringSpecification(criteria.getThumbnail(), BlogPost_.thumbnail),
                    buildStringSpecification(criteria.getStatus(), BlogPost_.status),
                    buildRangeSpecification(criteria.getViewCount(), BlogPost_.viewCount),
                    buildRangeSpecification(criteria.getPublishedAt(), BlogPost_.publishedAt),
                    buildRangeSpecification(criteria.getCreatedAt(), BlogPost_.createdAt),
                    buildRangeSpecification(criteria.getUpdatedAt(), BlogPost_.updatedAt),
                    buildStringSpecification(criteria.getAuthorName(), BlogPost_.authorName),
                    buildSpecification(criteria.getCategoryId(), root ->
                        root.join(BlogPost_.categories, JoinType.LEFT).get(BlogCategory_.id)
                    ),
                    buildSpecification(criteria.getTagId(), root -> root.join(BlogPost_.tags, JoinType.LEFT).get(Tag_.id))
                )
            );
        }
        return specification;
    }
}
