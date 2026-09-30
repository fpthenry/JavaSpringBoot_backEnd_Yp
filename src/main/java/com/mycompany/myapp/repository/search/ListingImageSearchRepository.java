package com.mycompany.myapp.repository.search;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import com.mycompany.myapp.domain.ListingImage;
import com.mycompany.myapp.repository.ListingImageRepository;
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
 * Spring Data Elasticsearch repository for the {@link ListingImage} entity.
 */
public interface ListingImageSearchRepository extends ElasticsearchRepository<ListingImage, Long>, ListingImageSearchRepositoryInternal {}

interface ListingImageSearchRepositoryInternal {
    Page<ListingImage> search(String query, Pageable pageable);

    Page<ListingImage> search(Query query);

    @Async
    void index(ListingImage entity);

    @Async
    void deleteFromIndexById(Long id);
}

class ListingImageSearchRepositoryInternalImpl implements ListingImageSearchRepositoryInternal {

    private final ElasticsearchTemplate elasticsearchTemplate;
    private final ListingImageRepository repository;

    ListingImageSearchRepositoryInternalImpl(ElasticsearchTemplate elasticsearchTemplate, ListingImageRepository repository) {
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.repository = repository;
    }

    @Override
    public Page<ListingImage> search(String query, Pageable pageable) {
        NativeQuery nativeQuery = new NativeQuery(QueryStringQuery.of(qs -> qs.query(query))._toQuery());
        return search(nativeQuery.setPageable(pageable));
    }

    @Override
    public Page<ListingImage> search(Query query) {
        SearchHits<ListingImage> searchHits = elasticsearchTemplate.search(query, ListingImage.class);
        List<ListingImage> hits = searchHits.map(SearchHit::getContent).stream().toList();
        return new PageImpl<>(hits, query.getPageable(), searchHits.getTotalHits());
    }

    @Override
    public void index(ListingImage entity) {
        repository.findOneWithEagerRelationships(entity.getId()).ifPresent(elasticsearchTemplate::save);
    }

    @Override
    public void deleteFromIndexById(Long id) {
        elasticsearchTemplate.delete(String.valueOf(id), ListingImage.class);
    }
}
