package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.*; // for static metamodels
import com.mycompany.myapp.domain.Listing;
import com.mycompany.myapp.repository.ListingRepository;
import com.mycompany.myapp.repository.LocationRepository;
import com.mycompany.myapp.repository.search.ListingSearchRepository;
import com.mycompany.myapp.service.criteria.ListingCriteria;
import com.mycompany.myapp.service.dto.ListingDTO;
import com.mycompany.myapp.service.mapper.ListingMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.sql.Statement;
import java.util.List;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.QueryUtils;
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

    /** Từ ngưỡng này (~5% tổng listing) lấy trang với semijoin=off, xem {@link #findInLocationTree}. */
    private static final long LARGE_LOCATION_TREE = 100_000;

    private final ListingRepository listingRepository;

    private final ListingMapper listingMapper;

    private final ListingSearchRepository listingSearchRepository;

    private final LocationRepository locationRepository;

    private final EntityManager entityManager;

    public ListingQueryService(
        ListingRepository listingRepository,
        ListingMapper listingMapper,
        ListingSearchRepository listingSearchRepository,
        LocationRepository locationRepository,
        EntityManager entityManager
    ) {
        this.listingRepository = listingRepository;
        this.listingMapper = listingMapper;
        this.listingSearchRepository = listingSearchRepository;
        this.locationRepository = locationRepository;
        this.entityManager = entityManager;
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
        if (hasLocationTree(criteria)) {
            return findInLocationTree(specification, page);
        }
        return listingRepository.fetchBagRelationships(listingRepository.findAll(specification, page)).map(listingMapper::toDto);
    }

    /**
     * Sửa tay: với bộ lọc cây địa phương, MySQL mặc định chọn semi-join (dựng cả tập con rồi mới sắp xếp):
     * nhanh cho COUNT và cho tập nhỏ, nhưng rất chậm khi lấy trang đủ cột của tập lớn (TP.HCM: 3-22 s).
     * Với tập lớn, tắt semi-join để MySQL duyệt listing theo thứ tự và dừng khi đủ một trang (TP.HCM: ~1 s).
     * Tập nhỏ thì không tắt: listing của một quận/huyện có thể nằm dồn ở cuối bảng (Huyện Ba Vì: id ~1,44 triệu),
     * duyệt từ đầu sẽ chậm hơn semi-join.
     */
    protected Page<ListingDTO> findInLocationTree(Specification<Listing> specification, Pageable page) {
        long total = listingRepository.count(specification);
        if (total < LARGE_LOCATION_TREE) {
            return listingRepository.fetchBagRelationships(listingRepository.findAll(specification, page)).map(listingMapper::toDto);
        }
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Listing> query = cb.createQuery(Listing.class);
        Root<Listing> root = query.from(Listing.class);
        query
            .select(root)
            .where(specification.toPredicate(root, query, cb))
            .orderBy(QueryUtils.toOrders(page.getSort(), root, cb));
        List<Listing> content;
        setSemijoin(false);
        try {
            content = entityManager
                .createQuery(query)
                .setFirstResult((int) page.getOffset())
                .setMaxResults(page.getPageSize())
                .getResultList();
        } finally {
            setSemijoin(true);
        }
        return listingRepository.fetchBagRelationships(new PageImpl<>(content, page, total)).map(listingMapper::toDto);
    }

    private static boolean hasLocationTree(ListingCriteria criteria) {
        return criteria != null && criteria.getLocationTreeId() != null && criteria.getLocationTreeId().getEquals() != null;
    }

    private void setSemijoin(boolean enabled) {
        String value = enabled ? "on" : "off";
        entityManager.unwrap(Session.class).doWork(connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET SESSION optimizer_switch = 'semijoin=" + value + "'");
            }
        });
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
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildRangeSpecification(criteria.getId(), Listing_.id),
                    buildRangeSpecification(criteria.getWpId(), Listing_.wpId),
                    buildStringSpecification(criteria.getApiId(), Listing_.apiId),
                    buildStringSpecification(criteria.getName(), Listing_.name),
                    buildStringSpecification(criteria.getNameEn(), Listing_.nameEn),
                    buildStringSpecification(criteria.getNameAlias(), Listing_.nameAlias),
                    buildStringSpecification(criteria.getSlug(), Listing_.slug),
                    buildStringSpecification(criteria.getPhone(), Listing_.phone),
                    buildStringSpecification(criteria.getMobile(), Listing_.mobile),
                    buildStringSpecification(criteria.getEmail(), Listing_.email),
                    buildStringSpecification(criteria.getWebsite(), Listing_.website),
                    buildStringSpecification(criteria.getTaxCode(), Listing_.taxCode),
                    buildStringSpecification(criteria.getRepresentative(), Listing_.representative),
                    buildStringSpecification(criteria.getCapital(), Listing_.capital),
                    buildStringSpecification(criteria.getFoundedYear(), Listing_.foundedYear),
                    buildStringSpecification(criteria.getBusinessType(), Listing_.businessType),
                    buildStringSpecification(criteria.getBusinessStatus(), Listing_.businessStatus),
                    buildStringSpecification(criteria.getIndustryCode(), Listing_.industryCode),
                    buildStringSpecification(criteria.getManagedBy(), Listing_.managedBy),
                    buildStringSpecification(criteria.getThumbnail(), Listing_.thumbnail),
                    buildRangeSpecification(criteria.getViewCount(), Listing_.viewCount),
                    buildSpecification(criteria.getIsFeatured(), Listing_.isFeatured),
                    buildStringSpecification(criteria.getStatus(), Listing_.status),
                    buildRangeSpecification(criteria.getPublishedAt(), Listing_.publishedAt),
                    buildRangeSpecification(criteria.getModifiedAt(), Listing_.modifiedAt),
                    buildRangeSpecification(criteria.getCreatedAt(), Listing_.createdAt),
                    buildRangeSpecification(criteria.getUpdatedAt(), Listing_.updatedAt),
                    buildSpecification(criteria.getEsIndexed(), Listing_.esIndexed),
                    buildSpecification(criteria.getCategoryId(), root -> root.join(Listing_.categories, JoinType.LEFT).get(Category_.id)),
                    buildSpecification(criteria.getLocationId(), root -> root.join(Listing_.locations, JoinType.LEFT).get(Location_.id))
                )
            );
            // Sửa tay: lọc theo cả cây địa phương
            if (criteria.getLocationTreeId() != null && criteria.getLocationTreeId().getEquals() != null) {
                specification = specification.and(inLocationTree(criteria.getLocationTreeId().getEquals()));
            }
        }
        return specification;
    }

    /**
     * Listing gắn vào địa phương {@code locationId} hoặc bất kỳ cấp con nào của nó.
     * Mỗi listing chỉ gắn vào một cấp (tỉnh, quận/huyện hoặc phường/xã), nên lọc theo tỉnh phải gồm cả cây con.
     * Dùng {@code exists} tương quan trên bảng nối: không bị trùng dòng, và chỉ đọc rel_listing__location
     * (nhanh gấp ~3 lần {@code id in (select ... join listing)} với tỉnh lớn như TP.HCM).
     */
    protected Specification<Listing> inLocationTree(Long locationId) {
        List<Long> locationIds = locationRepository.findSubtreeIds(locationId);
        return (root, query, cb) -> {
            if (locationIds.isEmpty()) {
                return cb.disjunction();
            }
            Subquery<Integer> linked = query.subquery(Integer.class);
            Root<Listing> listing = linked.correlate(root);
            linked.select(cb.literal(1)).where(listing.join(Listing_.locations).get(Location_.id).in(locationIds));
            return cb.exists(linked);
        };
    }
}
