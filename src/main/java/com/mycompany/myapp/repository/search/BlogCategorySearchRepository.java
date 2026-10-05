package com.mycompany.myapp.repository.search;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import com.mycompany.myapp.domain.BlogCategory;
import com.mycompany.myapp.repository.BlogCategoryRepository;
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
 * Spring Data Elasticsearch repository for the {@link BlogCategory} entity.
 */
public interface BlogCategorySearchRepository extends ElasticsearchRepository<BlogCategory, Long>, BlogCategorySearchRepositoryInternal {}

interface BlogCategorySearchRepositoryInternal {
    Page<BlogCategory> search(String query, Pageable pageable);

    Page<BlogCategory> search(Query query);

    @Async
    void index(BlogCategory entity);

    @Async
    void deleteFromIndexById(Long id);
}

class BlogCategorySearchRepositoryInternalImpl implements BlogCategorySearchRepositoryInternal {

    private final ElasticsearchTemplate elasticsearchTemplate;
    private final BlogCategoryRepository repository;

    BlogCategorySearchRepositoryInternalImpl(ElasticsearchTemplate elasticsearchTemplate, BlogCategoryRepository repository) {
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.repository = repository;
    }

    @Override
    public Page<BlogCategory> search(String query, Pageable pageable) {
        NativeQuery nativeQuery = new NativeQuery(QueryStringQuery.of(qs -> qs.query(query))._toQuery());
        return search(nativeQuery.setPageable(pageable));
    }

    @Override
    public Page<BlogCategory> search(Query query) {
        SearchHits<BlogCategory> searchHits = elasticsearchTemplate.search(query, BlogCategory.class);
        List<BlogCategory> hits = searchHits.map(SearchHit::getContent).stream().toList();
        return new PageImpl<>(hits, query.getPageable(), searchHits.getTotalHits());
    }

    @Override
    public void index(BlogCategory entity) {
        repository.findOneWithEagerRelationships(entity.getId()).ifPresent(elasticsearchTemplate::save);
    }

    @Override
    public void deleteFromIndexById(Long id) {
        elasticsearchTemplate.delete(String.valueOf(id), BlogCategory.class);
    }
}
