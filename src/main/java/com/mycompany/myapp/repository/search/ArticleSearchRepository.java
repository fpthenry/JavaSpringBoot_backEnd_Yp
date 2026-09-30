package com.mycompany.myapp.repository.search;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import com.mycompany.myapp.domain.Article;
import com.mycompany.myapp.repository.ArticleRepository;
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
 * Spring Data Elasticsearch repository for the {@link Article} entity.
 */
public interface ArticleSearchRepository extends ElasticsearchRepository<Article, Long>, ArticleSearchRepositoryInternal {}

interface ArticleSearchRepositoryInternal {
    Page<Article> search(String query, Pageable pageable);

    Page<Article> search(Query query);

    @Async
    void index(Article entity);

    @Async
    void deleteFromIndexById(Long id);
}

class ArticleSearchRepositoryInternalImpl implements ArticleSearchRepositoryInternal {

    private final ElasticsearchTemplate elasticsearchTemplate;
    private final ArticleRepository repository;

    ArticleSearchRepositoryInternalImpl(ElasticsearchTemplate elasticsearchTemplate, ArticleRepository repository) {
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.repository = repository;
    }

    @Override
    public Page<Article> search(String query, Pageable pageable) {
        NativeQuery nativeQuery = new NativeQuery(QueryStringQuery.of(qs -> qs.query(query))._toQuery());
        return search(nativeQuery.setPageable(pageable));
    }

    @Override
    public Page<Article> search(Query query) {
        SearchHits<Article> searchHits = elasticsearchTemplate.search(query, Article.class);
        List<Article> hits = searchHits.map(SearchHit::getContent).stream().toList();
        return new PageImpl<>(hits, query.getPageable(), searchHits.getTotalHits());
    }

    @Override
    public void index(Article entity) {
        repository.findOneWithEagerRelationships(entity.getId()).ifPresent(elasticsearchTemplate::save);
    }

    @Override
    public void deleteFromIndexById(Long id) {
        elasticsearchTemplate.delete(String.valueOf(id), Article.class);
    }
}
