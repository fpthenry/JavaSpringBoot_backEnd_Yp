package com.mycompany.myapp.repository.search;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import com.mycompany.myapp.domain.RedirectRule;
import com.mycompany.myapp.repository.RedirectRuleRepository;
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
 * Spring Data Elasticsearch repository for the {@link RedirectRule} entity.
 */
public interface RedirectRuleSearchRepository extends ElasticsearchRepository<RedirectRule, Long>, RedirectRuleSearchRepositoryInternal {}

interface RedirectRuleSearchRepositoryInternal {
    Page<RedirectRule> search(String query, Pageable pageable);

    Page<RedirectRule> search(Query query);

    @Async
    void index(RedirectRule entity);

    @Async
    void deleteFromIndexById(Long id);
}

class RedirectRuleSearchRepositoryInternalImpl implements RedirectRuleSearchRepositoryInternal {

    private final ElasticsearchTemplate elasticsearchTemplate;
    private final RedirectRuleRepository repository;

    RedirectRuleSearchRepositoryInternalImpl(ElasticsearchTemplate elasticsearchTemplate, RedirectRuleRepository repository) {
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.repository = repository;
    }

    @Override
    public Page<RedirectRule> search(String query, Pageable pageable) {
        NativeQuery nativeQuery = new NativeQuery(QueryStringQuery.of(qs -> qs.query(query))._toQuery());
        return search(nativeQuery.setPageable(pageable));
    }

    @Override
    public Page<RedirectRule> search(Query query) {
        SearchHits<RedirectRule> searchHits = elasticsearchTemplate.search(query, RedirectRule.class);
        List<RedirectRule> hits = searchHits.map(SearchHit::getContent).stream().toList();
        return new PageImpl<>(hits, query.getPageable(), searchHits.getTotalHits());
    }

    @Override
    public void index(RedirectRule entity) {
        repository.findById(entity.getId()).ifPresent(elasticsearchTemplate::save);
    }

    @Override
    public void deleteFromIndexById(Long id) {
        elasticsearchTemplate.delete(String.valueOf(id), RedirectRule.class);
    }
}
