package com.mycompany.myapp.repository.search;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import com.mycompany.myapp.domain.ArticleCategory;
import com.mycompany.myapp.repository.ArticleCategoryRepository;
import java.util.stream.Stream;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.scheduling.annotation.Async;

/**
 * Spring Data Elasticsearch repository for the {@link ArticleCategory} entity.
 */
public interface ArticleCategorySearchRepository
    extends ElasticsearchRepository<ArticleCategory, Long>, ArticleCategorySearchRepositoryInternal {}

interface ArticleCategorySearchRepositoryInternal {
    Stream<ArticleCategory> search(String query);

    Stream<ArticleCategory> search(Query query);

    @Async
    void index(ArticleCategory entity);

    @Async
    void deleteFromIndexById(Long id);
}

class ArticleCategorySearchRepositoryInternalImpl implements ArticleCategorySearchRepositoryInternal {

    private final ElasticsearchTemplate elasticsearchTemplate;
    private final ArticleCategoryRepository repository;

    ArticleCategorySearchRepositoryInternalImpl(ElasticsearchTemplate elasticsearchTemplate, ArticleCategoryRepository repository) {
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.repository = repository;
    }

    @Override
    public Stream<ArticleCategory> search(String query) {
        NativeQuery nativeQuery = new NativeQuery(QueryStringQuery.of(qs -> qs.query(query))._toQuery());
        return search(nativeQuery);
    }

    @Override
    public Stream<ArticleCategory> search(Query query) {
        return elasticsearchTemplate.search(query, ArticleCategory.class).map(SearchHit::getContent).stream();
    }

    @Override
    public void index(ArticleCategory entity) {
        repository.findOneWithEagerRelationships(entity.getId()).ifPresent(elasticsearchTemplate::save);
    }

    @Override
    public void deleteFromIndexById(Long id) {
        elasticsearchTemplate.delete(String.valueOf(id), ArticleCategory.class);
    }
}
