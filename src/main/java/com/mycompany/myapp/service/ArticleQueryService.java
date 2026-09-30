package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.*; // for static metamodels
import com.mycompany.myapp.domain.Article;
import com.mycompany.myapp.repository.ArticleRepository;
import com.mycompany.myapp.repository.search.ArticleSearchRepository;
import com.mycompany.myapp.service.criteria.ArticleCriteria;
import com.mycompany.myapp.service.dto.ArticleDTO;
import com.mycompany.myapp.service.mapper.ArticleMapper;
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
 * Service for executing complex queries for {@link Article} entities in the database.
 * The main input is a {@link ArticleCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link ArticleDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class ArticleQueryService extends QueryService<Article> {

    private static final Logger LOG = LoggerFactory.getLogger(ArticleQueryService.class);

    private final ArticleRepository articleRepository;

    private final ArticleMapper articleMapper;

    private final ArticleSearchRepository articleSearchRepository;

    public ArticleQueryService(
        ArticleRepository articleRepository,
        ArticleMapper articleMapper,
        ArticleSearchRepository articleSearchRepository
    ) {
        this.articleRepository = articleRepository;
        this.articleMapper = articleMapper;
        this.articleSearchRepository = articleSearchRepository;
    }

    /**
     * Return a {@link Page} of {@link ArticleDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<ArticleDTO> findByCriteria(ArticleCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Article> specification = createSpecification(criteria);
        return articleRepository.fetchBagRelationships(articleRepository.findAll(specification, page)).map(articleMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(ArticleCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Article> specification = createSpecification(criteria);
        return articleRepository.count(specification);
    }

    /**
     * Function to convert {@link ArticleCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Article> createSpecification(ArticleCriteria criteria) {
        Specification<Article> specification = Specification.unrestricted();
        specification = specification.and((root, query, builder) -> {
            if (Long.class != query.getResultType()) {
                root.fetch(Article_.author, JoinType.LEFT);
            }
            return null;
        });
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildRangeSpecification(criteria.getId(), Article_.id),
                    buildStringSpecification(criteria.getTitle(), Article_.title),
                    buildStringSpecification(criteria.getSlug(), Article_.slug),
                    buildStringSpecification(criteria.getStatus(), Article_.status),
                    buildRangeSpecification(criteria.getCreatedAt(), Article_.createdAt),
                    buildRangeSpecification(criteria.getUpdatedAt(), Article_.updatedAt),
                    buildSpecification(criteria.getAuthorId(), root -> root.join(Article_.author, JoinType.LEFT).get(User_.id)),
                    buildSpecification(criteria.getCategoriesId(), root ->
                        root.join(Article_.categorieses, JoinType.LEFT).get(ArticleCategory_.id)
                    ),
                    buildSpecification(criteria.getTagsId(), root -> root.join(Article_.tagses, JoinType.LEFT).get(ArticleTag_.id))
                )
            );
        }
        return specification;
    }
}
