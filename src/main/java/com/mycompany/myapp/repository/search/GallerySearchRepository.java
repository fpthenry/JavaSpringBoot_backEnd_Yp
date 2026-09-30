package com.mycompany.myapp.repository.search;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import com.mycompany.myapp.domain.Gallery;
import com.mycompany.myapp.repository.GalleryRepository;
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
 * Spring Data Elasticsearch repository for the {@link Gallery} entity.
 */
public interface GallerySearchRepository extends ElasticsearchRepository<Gallery, Long>, GallerySearchRepositoryInternal {}

interface GallerySearchRepositoryInternal {
    Page<Gallery> search(String query, Pageable pageable);

    Page<Gallery> search(Query query);

    @Async
    void index(Gallery entity);

    @Async
    void deleteFromIndexById(Long id);
}

class GallerySearchRepositoryInternalImpl implements GallerySearchRepositoryInternal {

    private final ElasticsearchTemplate elasticsearchTemplate;
    private final GalleryRepository repository;

    GallerySearchRepositoryInternalImpl(ElasticsearchTemplate elasticsearchTemplate, GalleryRepository repository) {
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.repository = repository;
    }

    @Override
    public Page<Gallery> search(String query, Pageable pageable) {
        NativeQuery nativeQuery = new NativeQuery(QueryStringQuery.of(qs -> qs.query(query))._toQuery());
        return search(nativeQuery.setPageable(pageable));
    }

    @Override
    public Page<Gallery> search(Query query) {
        SearchHits<Gallery> searchHits = elasticsearchTemplate.search(query, Gallery.class);
        List<Gallery> hits = searchHits.map(SearchHit::getContent).stream().toList();
        return new PageImpl<>(hits, query.getPageable(), searchHits.getTotalHits());
    }

    @Override
    public void index(Gallery entity) {
        repository.findOneWithEagerRelationships(entity.getId()).ifPresent(elasticsearchTemplate::save);
    }

    @Override
    public void deleteFromIndexById(Long id) {
        elasticsearchTemplate.delete(String.valueOf(id), Gallery.class);
    }
}
