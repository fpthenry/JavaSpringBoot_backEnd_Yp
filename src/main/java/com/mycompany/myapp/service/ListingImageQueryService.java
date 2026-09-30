package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.*; // for static metamodels
import com.mycompany.myapp.domain.ListingImage;
import com.mycompany.myapp.repository.ListingImageRepository;
import com.mycompany.myapp.repository.search.ListingImageSearchRepository;
import com.mycompany.myapp.service.criteria.ListingImageCriteria;
import com.mycompany.myapp.service.dto.ListingImageDTO;
import com.mycompany.myapp.service.mapper.ListingImageMapper;
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
 * Service for executing complex queries for {@link ListingImage} entities in the database.
 * The main input is a {@link ListingImageCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link ListingImageDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class ListingImageQueryService extends QueryService<ListingImage> {

    private static final Logger LOG = LoggerFactory.getLogger(ListingImageQueryService.class);

    private final ListingImageRepository listingImageRepository;

    private final ListingImageMapper listingImageMapper;

    private final ListingImageSearchRepository listingImageSearchRepository;

    public ListingImageQueryService(
        ListingImageRepository listingImageRepository,
        ListingImageMapper listingImageMapper,
        ListingImageSearchRepository listingImageSearchRepository
    ) {
        this.listingImageRepository = listingImageRepository;
        this.listingImageMapper = listingImageMapper;
        this.listingImageSearchRepository = listingImageSearchRepository;
    }

    /**
     * Return a {@link Page} of {@link ListingImageDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<ListingImageDTO> findByCriteria(ListingImageCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<ListingImage> specification = createSpecification(criteria);
        return listingImageRepository.findAll(specification, page).map(listingImageMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(ListingImageCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<ListingImage> specification = createSpecification(criteria);
        return listingImageRepository.count(specification);
    }

    /**
     * Function to convert {@link ListingImageCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<ListingImage> createSpecification(ListingImageCriteria criteria) {
        Specification<ListingImage> specification = Specification.unrestricted();
        specification = specification.and((root, query, builder) -> {
            if (Long.class != query.getResultType()) {
                root.fetch(ListingImage_.listing, JoinType.LEFT);
                root.fetch(ListingImage_.gallery, JoinType.LEFT);
            }
            return null;
        });
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildRangeSpecification(criteria.getId(), ListingImage_.id),
                    buildStringSpecification(criteria.getAltText(), ListingImage_.altText),
                    buildRangeSpecification(criteria.getDisplayOrder(), ListingImage_.displayOrder),
                    buildSpecification(criteria.getIsFeatured(), ListingImage_.isFeatured),
                    buildRangeSpecification(criteria.getCreatedAt(), ListingImage_.createdAt),
                    buildRangeSpecification(criteria.getUpdatedAt(), ListingImage_.updatedAt),
                    buildSpecification(criteria.getListingId(), root -> root.join(ListingImage_.listing, JoinType.LEFT).get(Listing_.id)),
                    buildSpecification(criteria.getGalleryId(), root -> root.join(ListingImage_.gallery, JoinType.LEFT).get(Gallery_.id))
                )
            );
        }
        return specification;
    }
}
