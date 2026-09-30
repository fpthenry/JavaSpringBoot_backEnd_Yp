package com.mycompany.myapp.repository.search;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import com.mycompany.myapp.domain.StaticPage;
import com.mycompany.myapp.repository.StaticPageRepository;
import java.util.stream.Stream;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.scheduling.annotation.Async;

/**
 * Spring Data Elasticsearch repository for the {@link StaticPage} entity.
 */
public interface StaticPageSearchRepository extends ElasticsearchRepository<StaticPage, Long>, StaticPageSearchRepositoryInternal {}

interface StaticPageSearchRepositoryInternal {
    Stream<StaticPage> search(String query);

    Stream<StaticPage> search(Query query);

    @Async
    void index(StaticPage entity);

    @Async
    void deleteFromIndexById(Long id);
}

class StaticPageSearchRepositoryInternalImpl implements StaticPageSearchRepositoryInternal {

    private final ElasticsearchTemplate elasticsearchTemplate;
    private final StaticPageRepository repository;

    StaticPageSearchRepositoryInternalImpl(ElasticsearchTemplate elasticsearchTemplate, StaticPageRepository repository) {
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.repository = repository;
    }

    @Override
    public Stream<StaticPage> search(String query) {
        NativeQuery nativeQuery = new NativeQuery(QueryStringQuery.of(qs -> qs.query(query))._toQuery());
        return search(nativeQuery);
    }

    @Override
    public Stream<StaticPage> search(Query query) {
        return elasticsearchTemplate.search(query, StaticPage.class).map(SearchHit::getContent).stream();
    }

    @Override
    public void index(StaticPage entity) {
        repository.findOneWithEagerRelationships(entity.getId()).ifPresent(elasticsearchTemplate::save);
    }

    @Override
    public void deleteFromIndexById(Long id) {
        elasticsearchTemplate.delete(String.valueOf(id), StaticPage.class);
    }
}
