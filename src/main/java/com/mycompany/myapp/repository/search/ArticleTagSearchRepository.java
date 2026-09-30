package com.mycompany.myapp.repository.search;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import com.mycompany.myapp.domain.ArticleTag;
import com.mycompany.myapp.repository.ArticleTagRepository;
import java.util.stream.Stream;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.scheduling.annotation.Async;

/**
 * Spring Data Elasticsearch repository for the {@link ArticleTag} entity.
 */
public interface ArticleTagSearchRepository extends ElasticsearchRepository<ArticleTag, Long>, ArticleTagSearchRepositoryInternal {}

interface ArticleTagSearchRepositoryInternal {
    Stream<ArticleTag> search(String query);

    Stream<ArticleTag> search(Query query);

    @Async
    void index(ArticleTag entity);

    @Async
    void deleteFromIndexById(Long id);
}

class ArticleTagSearchRepositoryInternalImpl implements ArticleTagSearchRepositoryInternal {

    private final ElasticsearchTemplate elasticsearchTemplate;
    private final ArticleTagRepository repository;

    ArticleTagSearchRepositoryInternalImpl(ElasticsearchTemplate elasticsearchTemplate, ArticleTagRepository repository) {
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.repository = repository;
    }

    @Override
    public Stream<ArticleTag> search(String query) {
        NativeQuery nativeQuery = new NativeQuery(QueryStringQuery.of(qs -> qs.query(query))._toQuery());
        return search(nativeQuery);
    }

    @Override
    public Stream<ArticleTag> search(Query query) {
        return elasticsearchTemplate.search(query, ArticleTag.class).map(SearchHit::getContent).stream();
    }

    @Override
    public void index(ArticleTag entity) {
        repository.findById(entity.getId()).ifPresent(elasticsearchTemplate::save);
    }

    @Override
    public void deleteFromIndexById(Long id) {
        elasticsearchTemplate.delete(String.valueOf(id), ArticleTag.class);
    }
}
