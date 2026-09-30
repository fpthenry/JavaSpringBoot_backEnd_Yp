package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.*; // for static metamodels
import com.mycompany.myapp.domain.Listing;
import com.mycompany.myapp.repository.ListingRepository;
import com.mycompany.myapp.repository.search.ListingSearchRepository;
import com.mycompany.myapp.service.criteria.ListingCriteria;
import com.mycompany.myapp.service.dto.ListingDTO;
import com.mycompany.myapp.service.mapper.ListingMapper;
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
 * Service for executing complex queries for {@link Listing} entities in the database.
 * The main input is a {@link ListingCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link ListingDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class ListingQueryService extends QueryService<Listing> {

    private static final Logger LOG = LoggerFactory.getLogger(ListingQueryService.class);

    private final ListingRepository listingRepository;

    private final ListingMapper listingMapper;

    private final ListingSearchRepository listingSearchRepository;

    public ListingQueryService(
        ListingRepository listingRepository,
        ListingMapper listingMapper,
        ListingSearchRepository listingSearchRepository
    ) {
        this.listingRepository = listingRepository;
        this.listingMapper = listingMapper;
        this.listingSearchRepository = listingSearchRepository;
    }

    /**
     * Return a {@link Page} of {@link ListingDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<ListingDTO> findByCriteria(ListingCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Listing> specification = createSpecification(criteria);
        return listingRepository.fetchBagRelationships(listingRepository.findAll(specification, page)).map(listingMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(ListingCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Listing> specification = createSpecification(criteria);
        return listingRepository.count(specification);
    }

    /**
     * Function to convert {@link ListingCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Listing> createSpecification(ListingCriteria criteria) {
        Specification<Listing> specification = Specification.unrestricted();
        specification = specification.and((root, query, builder) -> {
            if (Long.class != query.getResultType()) {
                root.fetch(Listing_.author, JoinType.LEFT);
            }
            return null;
        });
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildRangeSpecification(criteria.getId(), Listing_.id),
                    buildStringSpecification(criteria.getTitle(), Listing_.title),
                    buildStringSpecification(criteria.getSlug(), Listing_.slug),
                    buildStringSpecification(criteria.getStatus(), Listing_.status),
                    buildStringSpecification(criteria.getEmail(), Listing_.email),
                    buildStringSpecification(criteria.getTelephone(), Listing_.telephone),
                    buildStringSpecification(criteria.getMobile(), Listing_.mobile),
                    buildStringSpecification(criteria.getWebsite(), Listing_.website),
                    buildStringSpecification(criteria.getFax(), Listing_.fax),
                    buildStringSpecification(criteria.getTaxCode(), Listing_.taxCode),
                    buildStringSpecification(criteria.getNameAlias(), Listing_.nameAlias),
                    buildStringSpecification(criteria.getNameEn(), Listing_.nameEn),
                    buildStringSpecification(criteria.getRepresentative(), Listing_.representative),
                    buildStringSpecification(criteria.getMainIndustry(), Listing_.mainIndustry),
                    buildStringSpecification(criteria.getManagedBy(), Listing_.managedBy),
                    buildStringSpecification(criteria.getBusinessType(), Listing_.businessType),
                    buildStringSpecification(criteria.getStatusYp(), Listing_.statusYp),
                    buildRangeSpecification(criteria.getFoundedDate(), Listing_.foundedDate),
                    buildRangeSpecification(criteria.getLicenseModifiedDate(), Listing_.licenseModifiedDate),
                    buildRangeSpecification(criteria.getLatitude(), Listing_.latitude),
                    buildRangeSpecification(criteria.getLongitude(), Listing_.longitude),
                    buildRangeSpecification(criteria.getApiId(), Listing_.apiId),
                    buildRangeSpecification(criteria.getCreatedAt(), Listing_.createdAt),
                    buildRangeSpecification(criteria.getUpdatedAt(), Listing_.updatedAt),
                    buildSpecification(criteria.getImagesId(), root -> root.join(Listing_.imageses, JoinType.LEFT).get(ListingImage_.id)),
                    buildSpecification(criteria.getGalleriesId(), root -> root.join(Listing_.gallerieses, JoinType.LEFT).get(Gallery_.id)),
                    buildSpecification(criteria.getAuthorId(), root -> root.join(Listing_.author, JoinType.LEFT).get(User_.id)),
                    buildSpecification(criteria.getCategoriesId(), root ->
                        root.join(Listing_.categorieses, JoinType.LEFT).get(Category_.id)
                    ),
                    buildSpecification(criteria.getLocationsId(), root -> root.join(Listing_.locationses, JoinType.LEFT).get(Location_.id))
                )
            );
        }
        return specification;
    }
}
