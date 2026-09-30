package com.mycompany.myapp.repository.search;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import com.mycompany.myapp.domain.Listing;
import com.mycompany.myapp.repository.ListingRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.scheduling.annotation.Async;

/**
 * Spring Data Elasticsearch repository for the {@link Listing} entity.
 */
public interface ListingSearchRepository extends ElasticsearchRepository<Listing, Long>, ListingSearchRepositoryInternal {}

interface ListingSearchRepositoryInternal {
    Page<Listing> search(String query, Pageable pageable);

    Page<Listing> search(Query query);

    @Async
    void index(Listing entity);

    @Async
    void deleteFromIndexById(Long id);
}

class ListingSearchRepositoryInternalImpl implements ListingSearchRepositoryInternal {

    private final ElasticsearchTemplate elasticsearchTemplate;
    private final ListingRepository repository;

    ListingSearchRepositoryInternalImpl(ElasticsearchTemplate elasticsearchTemplate, ListingRepository repository) {
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.repository = repository;
    }

    @Override
    public Page<Listing> search(String query, Pageable pageable) {
        NativeQuery nativeQuery = new NativeQuery(QueryStringQuery.of(qs -> qs.query(query))._toQuery());
        return search(nativeQuery.setPageable(pageable));
    }

    @Override
    public Page<Listing> search(Query query) {
        SearchHits<Listing> searchHits = elasticsearchTemplate.search(query, Listing.class);
        List<Listing> hits = searchHits.map(SearchHit::getContent).stream().toList();
        return new PageImpl<>(hits, query.getPageable(), searchHits.getTotalHits());
    }

    @Override
    public void index(Listing entity) {
        repository.findOneWithEagerRelationships(entity.getId()).ifPresent(elasticsearchTemplate::save);
    }

    @Override
    public void deleteFromIndexById(Long id) {
        elasticsearchTemplate.delete(String.valueOf(id), Listing.class);
    }
}
