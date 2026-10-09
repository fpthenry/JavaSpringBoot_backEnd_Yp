package com.mycompany.myapp.repository.search;

import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import com.mycompany.myapp.domain.Listing;
import com.mycompany.myapp.repository.ListingRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilterBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.scheduling.annotation.Async;

/**
 * Spring Data Elasticsearch repository for the {@link Listing} entity.
 */
public interface ListingSearchRepository extends ElasticsearchRepository<Listing, Long>, ListingSearchRepositoryInternal {}

interface ListingSearchRepositoryInternal {
    Page<Listing> search(String query, Pageable pageable);

    Page<Listing> search(ListingSearchFilter filter, Pageable pageable);

    long count(ListingSearchFilter filter);

    Page<Listing> search(Query query);

    /** Sửa tay: entity phải được điền sẵn field tìm kiếm bằng {@link ListingSearchFields#fill(Listing)}. */
    @Async
    void index(Listing entity);

    @Async
    void deleteFromIndexById(Long id);
}

/**
 * Sửa tay so với bản JHipster sinh ra:
 * <ul>
 *   <li>Từ khóa: query_string, phải có đủ mọi chữ, tìm trên mọi field (địa chỉ cũ và mới); đếm tổng chính xác.</li>
 *   <li>Lọc theo cây địa phương, ngành nghề bằng {@code locationIds}, {@code categoryIds} (đã gồm các cấp cha), thay cho
 *   truy vấn MySQL trên bảng nối 25 triệu dòng (nhóm lớn mất 9–45 s).</li>
 *   <li>ES chỉ trả id; doanh nghiệp đọc lại từ MySQL kèm ngành, địa phương, giữ thứ tự của ES.</li>
 *   <li>index() ghi nguyên entity đã được service điền sẵn field tìm kiếm, không đọc lại DB (chạy @Async, lúc đó
 *   transaction lưu có thể chưa commit).</li>
 * </ul>
 */
class ListingSearchRepositoryInternalImpl implements ListingSearchRepositoryInternal {

    /** Field số, ngày: sắp xếp trực tiếp. Field chữ khác sắp theo {@code .keyword}. */
    static final Set<String> NATIVE_SORT_FIELDS = Set.of(
        "wpId",
        "viewCount",
        "isFeatured",
        "esIndexed",
        "createdAt",
        "updatedAt",
        "publishedAt",
        "modifiedAt"
    );

    private final ElasticsearchTemplate elasticsearchTemplate;
    private final ListingRepository repository;

    ListingSearchRepositoryInternalImpl(ElasticsearchTemplate elasticsearchTemplate, ListingRepository repository) {
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.repository = repository;
    }

    @Override
    public Page<Listing> search(String query, Pageable pageable) {
        return search(new ListingSearchFilter(query, null, null), pageable);
    }

    @Override
    public Page<Listing> search(ListingSearchFilter filter, Pageable pageable) {
        NativeQuery nativeQuery = NativeQuery.builder()
            .withQuery(buildQuery(filter))
            .withSourceFilter(new FetchSourceFilterBuilder().withIncludes("id").build())
            .withPageable(toEsPageable(pageable))
            .withTrackTotalHits(true)
            .build();
        SearchHits<Listing> hits = elasticsearchTemplate.search(nativeQuery, Listing.class);
        List<Long> ids = hits
            .stream()
            .map(hit -> Long.valueOf(hit.getId()))
            .toList();
        return new PageImpl<>(loadInOrder(ids), pageable, hits.getTotalHits());
    }

    @Override
    public long count(ListingSearchFilter filter) {
        return elasticsearchTemplate.count(NativeQuery.builder().withQuery(buildQuery(filter)).build(), Listing.class);
    }

    static co.elastic.clients.elasticsearch._types.query_dsl.Query buildQuery(ListingSearchFilter filter) {
        return co.elastic.clients.elasticsearch._types.query_dsl.Query.of(q ->
            q.bool(b -> {
                if (filter.hasQuery()) {
                    b.must(m ->
                        m.queryString(
                            QueryStringQuery.of(qs -> qs.query(filter.query().trim()).defaultOperator(Operator.And).lenient(true))
                        )
                    );
                } else {
                    b.must(m -> m.matchAll(all -> all));
                }
                if (filter.locationTreeId() != null) {
                    b.filter(f -> f.term(t -> t.field("locationIds").value(filter.locationTreeId())));
                }
                if (filter.categoryTreeId() != null) {
                    b.filter(f -> f.term(t -> t.field("categoryIds").value(filter.categoryTreeId())));
                }
                return b;
            })
        );
    }

    /** id → sortId (số); field số/ngày giữ nguyên; field chữ → .keyword. */
    static Pageable toEsPageable(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return pageable;
        }
        List<Sort.Order> orders = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            String property = order.getProperty();
            String field = "id".equals(property) ? "sortId" : NATIVE_SORT_FIELDS.contains(property) ? property : property + ".keyword";
            orders.add(new Sort.Order(order.getDirection(), field));
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(orders));
    }

    /** Đọc doanh nghiệp từ MySQL kèm ngành, địa phương, giữ thứ tự của ES. Bản ghi đã xóa khỏi DB mà còn trong index thì bỏ qua. */
    private List<Listing> loadInOrder(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, Listing> byId = repository
            .fetchBagRelationships(repository.findAllById(ids))
            .stream()
            .collect(Collectors.toMap(Listing::getId, Function.identity()));
        return ids.stream().map(byId::get).filter(Objects::nonNull).toList();
    }

    @Override
    public Page<Listing> search(Query query) {
        SearchHits<Listing> searchHits = elasticsearchTemplate.search(query, Listing.class);
        List<Listing> hits = searchHits
            .stream()
            .map(hit -> hit.getContent())
            .toList();
        return new PageImpl<>(hits, query.getPageable(), searchHits.getTotalHits());
    }

    @Override
    public void index(Listing entity) {
        elasticsearchTemplate.save(entity);
    }

    @Override
    public void deleteFromIndexById(Long id) {
        elasticsearchTemplate.delete(String.valueOf(id), Listing.class);
    }
}
