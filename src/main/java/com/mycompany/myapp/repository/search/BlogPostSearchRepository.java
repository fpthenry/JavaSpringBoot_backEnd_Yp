package com.mycompany.myapp.repository.search;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import com.mycompany.myapp.domain.BlogPost;
import com.mycompany.myapp.repository.BlogPostRepository;
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
 * Spring Data Elasticsearch repository for the {@link BlogPost} entity.
 */
public interface BlogPostSearchRepository extends ElasticsearchRepository<BlogPost, Long>, BlogPostSearchRepositoryInternal {}

interface BlogPostSearchRepositoryInternal {
    Page<BlogPost> search(String query, Pageable pageable);

    Page<BlogPost> search(Query query);

    @Async
    void index(BlogPost entity);

    @Async
    void deleteFromIndexById(Long id);
}

class BlogPostSearchRepositoryInternalImpl implements BlogPostSearchRepositoryInternal {

    private final ElasticsearchTemplate elasticsearchTemplate;
    private final BlogPostRepository repository;

    BlogPostSearchRepositoryInternalImpl(ElasticsearchTemplate elasticsearchTemplate, BlogPostRepository repository) {
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.repository = repository;
    }

    @Override
    public Page<BlogPost> search(String query, Pageable pageable) {
        NativeQuery nativeQuery = new NativeQuery(QueryStringQuery.of(qs -> qs.query(query))._toQuery());
        return search(nativeQuery.setPageable(pageable));
    }

    @Override
    public Page<BlogPost> search(Query query) {
        SearchHits<BlogPost> searchHits = elasticsearchTemplate.search(query, BlogPost.class);
        List<BlogPost> hits = searchHits.map(SearchHit::getContent).stream().toList();
        return new PageImpl<>(hits, query.getPageable(), searchHits.getTotalHits());
    }

    @Override
    public void index(BlogPost entity) {
        repository.findById(entity.getId()).ifPresent(elasticsearchTemplate::save);
    }

    @Override
    public void deleteFromIndexById(Long id) {
        elasticsearchTemplate.delete(String.valueOf(id), BlogPost.class);
    }
}
