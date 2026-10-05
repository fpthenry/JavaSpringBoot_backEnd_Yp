package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.*; // for static metamodels
import com.mycompany.myapp.domain.GalleryImage;
import com.mycompany.myapp.repository.GalleryImageRepository;
import com.mycompany.myapp.service.criteria.GalleryImageCriteria;
import com.mycompany.myapp.service.dto.GalleryImageDTO;
import com.mycompany.myapp.service.mapper.GalleryImageMapper;
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
 * Service for executing complex queries for {@link GalleryImage} entities in the database.
 * The main input is a {@link GalleryImageCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link GalleryImageDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class GalleryImageQueryService extends QueryService<GalleryImage> {

    private static final Logger LOG = LoggerFactory.getLogger(GalleryImageQueryService.class);

    private final GalleryImageRepository galleryImageRepository;

    private final GalleryImageMapper galleryImageMapper;

    public GalleryImageQueryService(GalleryImageRepository galleryImageRepository, GalleryImageMapper galleryImageMapper) {
        this.galleryImageRepository = galleryImageRepository;
        this.galleryImageMapper = galleryImageMapper;
    }

    /**
     * Return a {@link Page} of {@link GalleryImageDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<GalleryImageDTO> findByCriteria(GalleryImageCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<GalleryImage> specification = createSpecification(criteria);
        return galleryImageRepository.findAll(specification, page).map(galleryImageMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(GalleryImageCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<GalleryImage> specification = createSpecification(criteria);
        return galleryImageRepository.count(specification);
    }

    /**
     * Function to convert {@link GalleryImageCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<GalleryImage> createSpecification(GalleryImageCriteria criteria) {
        Specification<GalleryImage> specification = Specification.unrestricted();
        specification = specification.and((root, query, builder) -> {
            if (Long.class != query.getResultType()) {
                root.fetch(GalleryImage_.gallery, JoinType.LEFT);
            }
            return null;
        });
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildRangeSpecification(criteria.getId(), GalleryImage_.id),
                    buildStringSpecification(criteria.getTitle(), GalleryImage_.title),
                    buildStringSpecification(criteria.getImageUrl(), GalleryImage_.imageUrl),
                    buildStringSpecification(criteria.getLinkUrl(), GalleryImage_.linkUrl),
                    buildStringSpecification(criteria.getAltText(), GalleryImage_.altText),
                    buildRangeSpecification(criteria.getDisplayOrder(), GalleryImage_.displayOrder),
                    buildSpecification(criteria.getActive(), GalleryImage_.active),
                    buildRangeSpecification(criteria.getStartAt(), GalleryImage_.startAt),
                    buildRangeSpecification(criteria.getEndAt(), GalleryImage_.endAt),
                    buildSpecification(criteria.getOpenInNewTab(), GalleryImage_.openInNewTab),
                    buildSpecification(criteria.getGalleryId(), root -> root.join(GalleryImage_.gallery, JoinType.LEFT).get(Gallery_.id))
                )
            );
        }
        return specification;
    }
}
