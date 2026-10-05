package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.*; // for static metamodels
import com.mycompany.myapp.domain.Gallery;
import com.mycompany.myapp.repository.GalleryRepository;
import com.mycompany.myapp.service.criteria.GalleryCriteria;
import com.mycompany.myapp.service.dto.GalleryDTO;
import com.mycompany.myapp.service.mapper.GalleryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link Gallery} entities in the database.
 * The main input is a {@link GalleryCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link GalleryDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class GalleryQueryService extends QueryService<Gallery> {

    private static final Logger LOG = LoggerFactory.getLogger(GalleryQueryService.class);

    private final GalleryRepository galleryRepository;

    private final GalleryMapper galleryMapper;

    public GalleryQueryService(GalleryRepository galleryRepository, GalleryMapper galleryMapper) {
        this.galleryRepository = galleryRepository;
        this.galleryMapper = galleryMapper;
    }

    /**
     * Return a {@link Page} of {@link GalleryDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<GalleryDTO> findByCriteria(GalleryCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Gallery> specification = createSpecification(criteria);
        return galleryRepository.findAll(specification, page).map(galleryMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(GalleryCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Gallery> specification = createSpecification(criteria);
        return galleryRepository.count(specification);
    }

    /**
     * Function to convert {@link GalleryCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Gallery> createSpecification(GalleryCriteria criteria) {
        Specification<Gallery> specification = Specification.unrestricted();
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildRangeSpecification(criteria.getId(), Gallery_.id),
                    buildStringSpecification(criteria.getName(), Gallery_.name),
                    buildStringSpecification(criteria.getCode(), Gallery_.code),
                    buildSpecification(criteria.getActive(), Gallery_.active),
                    buildRangeSpecification(criteria.getWpId(), Gallery_.wpId)
                )
            );
        }
        return specification;
    }
}
