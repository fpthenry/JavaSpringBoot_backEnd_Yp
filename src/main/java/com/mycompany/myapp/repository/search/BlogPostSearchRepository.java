package com.mycompany.myapp.repository.search;

import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.mycompany.myapp.domain.BlogPost;
import com.mycompany.myapp.repository.BlogPostRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilterBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.StringUtils;

/**
 * Spring Data Elasticsearch repository for the {@link BlogPost} entity.
 */
public interface BlogPostSearchRepository extends ElasticsearchRepository<BlogPost, Long>, BlogPostSearchRepositoryInternal {}

interface BlogPostSearchRepositoryInternal {
    Page<BlogPost> search(String query, Pageable pageable);

    Page<BlogPost> search(BlogPostSearchFilter filter, Pageable pageable);

    Page<BlogPost> search(Query query);

    /** Sửa tay: entity phải được điền sẵn field tìm kiếm bằng {@link BlogPostSearchFields#fill(BlogPost)}. */
    @Async
    void index(BlogPost entity);

    @Async
    void deleteFromIndexById(Long id);
}

/**
 * Sửa tay so với bản JHipster sinh ra:
 * <ul>
 *   <li>Tìm theo từng từ thay cho query_string, phải có đủ mọi từ: từ có dấu khớp đúng dấu (field .exact),
 *   từ không dấu khớp mọi dấu; khớp cả cụm ở tiêu đề xếp trước. Truy vấn và dữ liệu đều qua {@link VietnameseText#normalize}.</li>
 *   <li>Lọc theo danh mục (gồm danh mục con), thẻ, trạng thái.</li>
 *   <li>Elasticsearch chỉ trả id; bài viết đọc lại từ MySQL kèm danh mục, thẻ (ES không lưu quan hệ).</li>
 *   <li>index() ghi nguyên entity đã được service điền sẵn field tìm kiếm, không đọc lại DB: chạy @Async nên lúc đó
 *   transaction lưu bài có thể chưa commit (bản JHipster đọc lại DB nên index nhầm bản cũ hoặc bỏ sót bài mới).</li>
 * </ul>
 */
class BlogPostSearchRepositoryInternalImpl implements BlogPostSearchRepositoryInternal {

    /** Field cho từ không dấu: khớp mọi dấu (khuyen = khuyến = khuyên). */
    static final List<String> FOLDED_FIELDS = List.of("title^3", "excerpt^2", "content", "categoryNames^2", "tagNames^2");
    /** Field cho từ có dấu: phải đúng dấu (đá không khớp da, đa). */
    static final List<String> EXACT_FIELDS = List.of(
        "title.exact^3",
        "excerpt.exact^2",
        "content.exact",
        "categoryNames.exact^2",
        "tagNames.exact^2"
    );

    private final ElasticsearchTemplate elasticsearchTemplate;
    private final BlogPostRepository repository;

    BlogPostSearchRepositoryInternalImpl(ElasticsearchTemplate elasticsearchTemplate, BlogPostRepository repository) {
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.repository = repository;
    }

    @Override
    public Page<BlogPost> search(String query, Pageable pageable) {
        return search(BlogPostSearchFilter.ofQuery(query), pageable);
    }

    @Override
    public Page<BlogPost> search(BlogPostSearchFilter filter, Pageable pageable) {
        Pageable paging = pageable;
        // Không có từ khóa và không chỉ định sắp xếp: bài mới đăng trước. Có từ khóa thì theo độ liên quan.
        if (!StringUtils.hasText(filter.query()) && pageable.getSort().isUnsorted()) {
            paging = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id"))
            );
        }
        NativeQuery nativeQuery = NativeQuery.builder()
            .withQuery(buildQuery(filter))
            .withSourceFilter(new FetchSourceFilterBuilder().withIncludes("id").build())
            .withPageable(paging)
            .withTrackTotalHits(true)
            .build();
        return search(nativeQuery);
    }

    static co.elastic.clients.elasticsearch._types.query_dsl.Query buildQuery(BlogPostSearchFilter filter) {
        String text = filter.query() == null ? "" : VietnameseText.normalize(filter.query().trim());
        List<String> words = VietnameseText.words(text);
        boolean anyAccent = words.stream().anyMatch(VietnameseText::hasDiacritics);
        return co.elastic.clients.elasticsearch._types.query_dsl.Query.of(q ->
            q.bool(b -> {
                if (words.isEmpty()) {
                    b.must(m -> m.matchAll(all -> all));
                } else {
                    // Mỗi từ phải có ở ít nhất một field; từ có dấu tìm trên field giữ dấu, từ không dấu trên field bỏ dấu
                    for (String word : words) {
                        List<String> fields = VietnameseText.hasDiacritics(word) ? EXACT_FIELDS : FOLDED_FIELDS;
                        b.must(m -> m.multiMatch(mm -> mm.query(word).fields(fields).type(TextQueryType.BestFields).tieBreaker(0.3)));
                    }
                    String phrase = String.join(" ", words);
                    b.should(s ->
                        s.matchPhrase(mp ->
                            mp
                                .field(anyAccent ? "title.exact" : "title")
                                .query(phrase)
                                .boost(3f)
                        )
                    );
                }
                if (filter.categoryId() != null) {
                    b.filter(f -> f.term(t -> t.field("categoryIds").value(filter.categoryId())));
                }
                if (filter.tagId() != null) {
                    b.filter(f -> f.term(t -> t.field("tagIds").value(filter.tagId())));
                }
                if (StringUtils.hasText(filter.status())) {
                    b.filter(f -> f.term(t -> t.field("status.keyword").value(filter.status().trim())));
                }
                return b;
            })
        );
    }

    @Override
    public Page<BlogPost> search(Query query) {
        SearchHits<BlogPost> searchHits = elasticsearchTemplate.search(query, BlogPost.class);
        List<Long> ids = searchHits
            .stream()
            .map(hit -> Long.valueOf(hit.getId()))
            .toList();
        return new PageImpl<>(loadInOrder(ids), query.getPageable(), searchHits.getTotalHits());
    }

    /** Đọc bài từ MySQL kèm danh mục, thẻ, giữ thứ tự của Elasticsearch. Bài đã xóa khỏi DB mà còn trong index thì bỏ qua. */
    private List<BlogPost> loadInOrder(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, BlogPost> byId = repository
            .fetchBagRelationships(repository.findAllById(ids))
            .stream()
            .collect(Collectors.toMap(BlogPost::getId, Function.identity()));
        return ids.stream().map(byId::get).filter(Objects::nonNull).toList();
    }

    @Override
    public void index(BlogPost entity) {
        elasticsearchTemplate.save(entity);
    }

    @Override
    public void deleteFromIndexById(Long id) {
        elasticsearchTemplate.delete(String.valueOf(id), BlogPost.class);
    }
}
