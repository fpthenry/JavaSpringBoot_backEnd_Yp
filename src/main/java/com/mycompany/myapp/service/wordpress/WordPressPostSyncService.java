package com.mycompany.myapp.service.wordpress;

import com.mycompany.myapp.domain.BlogCategory;
import com.mycompany.myapp.domain.BlogPost;
import com.mycompany.myapp.domain.Tag;
import com.mycompany.myapp.service.ElasticsearchReindexService;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityManager;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.util.HtmlUtils;
import tools.jackson.databind.JsonNode;

/**
 * Đồng bộ bài viết, danh mục và thẻ từ WordPress (REST API công khai) sang BlogPost, BlogCategory, Tag.
 * <ul>
 *   <li>Thêm mới hoặc cập nhật theo id WordPress ({@code wpId}, {@code wpTermId}); <b>không xóa gì</b>.
 *   Chạy lại nhiều lần được.</li>
 *   <li>Thứ tự: danh mục (rồi gán cha) → thẻ → bài viết (mỗi trang 100 bài một transaction) → reindex Elasticsearch.</li>
 *   <li>Chỉ đọc được bài đã xuất bản (bài nháp cần đăng nhập WordPress).</li>
 *   <li>Chạy nền trên một luồng riêng, chỉ một job tại một thời điểm.</li>
 * </ul>
 */
@Service
public class WordPressPostSyncService {

    private static final Logger LOG = LoggerFactory.getLogger(WordPressPostSyncService.class);

    private final WordPressClient client;
    private final EntityManager entityManager;
    private final TransactionTemplate transaction;
    private final ElasticsearchReindexService reindexService;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "wordpress-sync");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile SyncStatus status = SyncStatus.idle();

    public WordPressPostSyncService(
        WordPressClient client,
        EntityManager entityManager,
        PlatformTransactionManager transactionManager,
        ElasticsearchReindexService reindexService
    ) {
        this.client = client;
        this.entityManager = entityManager;
        this.transaction = new TransactionTemplate(transactionManager);
        this.reindexService = reindexService;
    }

    /** Bắt đầu đồng bộ ở chế độ nền. @throws IllegalStateException nếu đang có job chạy. */
    public SyncStatus start() {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Đang có job đồng bộ WordPress chạy");
        }
        status = SyncStatus.started();
        executor.submit(this::run);
        return status;
    }

    public SyncStatus getStatus() {
        return status;
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    private void run() {
        try {
            syncCategories();
            syncTags();
            syncPosts();
            status = status.with("reindex", s -> s);
            startReindex();
            status = status.finished(null);
            LOG.info("Đồng bộ WordPress xong: {}", status);
        } catch (Exception e) {
            LOG.error("Đồng bộ WordPress lỗi", e);
            status = status.finished(e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            running.set(false);
        }
    }

    // ---------------------------------------------------------------- danh mục

    void syncCategories() {
        status = status.with("categories", s -> s);
        Map<Long, Long> parentByWpId = new HashMap<>();
        Counter counter = new Counter();
        client.forEachPage("/wp/v2/categories", null, page ->
            transaction.executeWithoutResult(tx -> {
                Map<Long, BlogCategory> existing = findByWpIds(BlogCategory.class, "wpTermId", wpIds(page.items()));
                for (JsonNode node : page.items()) {
                    long wpId = node.path("id").asLong();
                    BlogCategory category = existing.get(wpId);
                    boolean isNew = category == null;
                    if (isNew) {
                        category = new BlogCategory();
                        category.setWpTermId(wpId);
                    }
                    category.setName(truncate(unescape(text(node, "name")), 255));
                    category.setSlug(truncate(decodeSlug(text(node, "slug")), 255));
                    category.setDescription(text(node, "description"));
                    category.setPostCount(node.path("count").asInt());
                    if (isNew) {
                        entityManager.persist(category);
                    }
                    counter.count(isNew);
                    long parent = node.path("parent").asLong();
                    parentByWpId.put(wpId, parent == 0 ? null : parent);
                }
                entityManager.flush();
                entityManager.clear();
            })
        );
        // Lượt 2: gán cha khi đã có đủ danh mục
        transaction.executeWithoutResult(tx -> {
            Map<Long, BlogCategory> all = findByWpIds(BlogCategory.class, "wpTermId", parentByWpId.keySet());
            parentByWpId.forEach((wpId, parentWpId) -> all.get(wpId).setParent(parentWpId == null ? null : all.get(parentWpId)));
        });
        status = status.with("categories", s -> s.withCategories(counter.created, counter.updated));
    }

    // ---------------------------------------------------------------- thẻ

    void syncTags() {
        status = status.with("tags", s -> s);
        Counter counter = new Counter();
        client.forEachPage("/wp/v2/tags", null, page ->
            transaction.executeWithoutResult(tx -> {
                Map<Long, Tag> existing = findByWpIds(Tag.class, "wpTermId", wpIds(page.items()));
                for (JsonNode node : page.items()) {
                    long wpId = node.path("id").asLong();
                    Tag tag = existing.get(wpId);
                    boolean isNew = tag == null;
                    if (isNew) {
                        tag = new Tag();
                        tag.setWpTermId(wpId);
                    }
                    tag.setName(truncate(unescape(text(node, "name")), 255));
                    tag.setSlug(truncate(decodeSlug(text(node, "slug")), 255));
                    if (isNew) {
                        entityManager.persist(tag);
                    }
                    counter.count(isNew);
                }
                entityManager.flush();
                entityManager.clear();
            })
        );
        status = status.with("tags", s -> s.withTags(counter.created, counter.updated));
    }

    // ---------------------------------------------------------------- bài viết

    void syncPosts() {
        status = status.with("posts", s -> s);
        Counter counter = new Counter();
        Set<Long> seenWpIds = new HashSet<>();
        client.forEachPage("/wp/v2/posts", "_embed=author,wp:featuredmedia", page -> {
            transaction.executeWithoutResult(tx -> {
                List<Long> ids = wpIds(page.items());
                seenWpIds.addAll(ids);
                Map<Long, BlogPost> existing = findByWpIds(BlogPost.class, "wpId", ids);
                Map<Long, BlogCategory> categories = findByWpIds(BlogCategory.class, "wpTermId", termIds(page.items(), "categories"));
                Map<Long, Tag> tags = findByWpIds(Tag.class, "wpTermId", termIds(page.items(), "tags"));
                for (JsonNode node : page.items()) {
                    long wpId = node.path("id").asLong();
                    BlogPost post = existing.get(wpId);
                    boolean isNew = post == null;
                    if (isNew) {
                        post = new BlogPost();
                        post.setWpId(wpId);
                        post.setViewCount(0);
                    }
                    applyPost(node, post, isNew);
                    post.setCategories(pick(node.path("categories"), categories));
                    post.setTags(pick(node.path("tags"), tags));
                    if (isNew) {
                        entityManager.persist(post);
                    }
                    counter.count(isNew);
                }
                entityManager.flush();
                entityManager.clear();
            });
            status = status.with("posts", s -> s.withPosts(counter.created, counter.updated, page.total()));
        });
        long localOnly = transaction.execute(tx ->
            entityManager
                .createQuery("select count(p) from BlogPost p where p.wpId not in :seen", Long.class)
                .setParameter("seen", seenWpIds.isEmpty() ? Set.of(-1L) : seenWpIds)
                .getSingleResult()
        );
        status = status.with("posts", s -> s.withLocalOnly(localOnly));
    }

    private static void applyPost(JsonNode node, BlogPost post, boolean isNew) {
        String title = unescape(text(node.path("title"), "rendered"));
        post.setTitle(truncate(title == null || title.isBlank() ? "(không tiêu đề)" : title, 500));
        post.setSlug(truncate(decodeSlug(text(node, "slug")), 500));
        post.setContent(text(node.path("content"), "rendered"));
        post.setExcerpt(text(node.path("excerpt"), "rendered"));
        post.setStatus(truncate(text(node, "status"), 50));
        Instant published = parseGmt(text(node, "date_gmt"));
        post.setPublishedAt(published);
        post.setUpdatedAt(parseGmt(text(node, "modified_gmt")));
        if (isNew || post.getCreatedAt() == null) {
            post.setCreatedAt(published);
        }
        JsonNode embedded = node.path("_embedded");
        String thumbnail = text(embedded.path("wp:featuredmedia").path(0), "source_url");
        post.setThumbnail(thumbnail != null && thumbnail.length() <= 500 ? thumbnail : null);
        post.setAuthorName(truncate(unescape(text(embedded.path("author").path(0), "name")), 255));
    }

    // ---------------------------------------------------------------- tiện ích

    private void startReindex() {
        try {
            reindexService.start(List.of("blogcategory", "tag", "blogpost"));
        } catch (IllegalStateException e) {
            LOG.warn(
                "Không reindex được vì đang có job reindex khác; chạy lại sau: POST /api/admin/elasticsearch/reindex?entities=blogcategory,tag,blogpost"
            );
        }
    }

    private <T> Map<Long, T> findByWpIds(Class<T> type, String wpIdField, java.util.Collection<Long> wpIds) {
        if (wpIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, T> result = new HashMap<>();
        for (List<Long> chunk : chunks(List.copyOf(wpIds), 1000)) {
            List<Object[]> rows = entityManager
                .createQuery(
                    "select e." + wpIdField + ", e from " + type.getSimpleName() + " e where e." + wpIdField + " in :ids",
                    Object[].class
                )
                .setParameter("ids", chunk)
                .getResultList();
            rows.forEach(row -> result.put((Long) row[0], type.cast(row[1])));
        }
        return result;
    }

    private static <T> List<List<T>> chunks(List<T> list, int size) {
        List<List<T>> result = new java.util.ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(list.size(), i + size)));
        }
        return result;
    }

    private static List<Long> wpIds(List<JsonNode> items) {
        return items
            .stream()
            .map(node -> node.path("id").asLong())
            .toList();
    }

    private static Set<Long> termIds(List<JsonNode> items, String field) {
        Set<Long> ids = new HashSet<>();
        items.forEach(node -> node.path(field).forEach(id -> ids.add(id.asLong())));
        return ids;
    }

    private static <T> Set<T> pick(JsonNode wpIds, Map<Long, T> byWpId) {
        Set<T> result = new HashSet<>();
        wpIds.forEach(id -> {
            T value = byWpId.get(id.asLong());
            if (value != null) {
                result.add(value);
            }
        });
        return result;
    }

    static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asString();
    }

    static String unescape(String html) {
        return html == null ? null : HtmlUtils.htmlUnescape(html).trim();
    }

    /** Slug WordPress có thể bị mã hóa %xx (slug có ký tự Unicode). */
    static String decodeSlug(String slug) {
        if (slug == null) {
            return null;
        }
        try {
            return URLDecoder.decode(slug, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return slug;
        }
    }

    static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    /** date_gmt của WordPress không có múi giờ, ví dụ 2026-10-05T07:09:50 (là giờ UTC). */
    static Instant parseGmt(String value) {
        return value == null || value.isBlank() || value.startsWith("0000") ? null : Instant.parse(value + "Z");
    }

    private static final class Counter {

        int created;
        int updated;

        void count(boolean isNew) {
            if (isNew) {
                created++;
            } else {
                updated++;
            }
        }
    }

    /** Trạng thái job (bất biến, thay cả object khi cập nhật). */
    public record SyncStatus(
        String state,
        String phase,
        int categoriesCreated,
        int categoriesUpdated,
        int tagsCreated,
        int tagsUpdated,
        int postsCreated,
        int postsUpdated,
        int postsTotalInWordPress,
        long postsOnlyInProject,
        Instant startedAt,
        Instant finishedAt,
        String error
    ) {
        static SyncStatus idle() {
            return new SyncStatus("IDLE", null, 0, 0, 0, 0, 0, 0, 0, 0, null, null, null);
        }

        static SyncStatus started() {
            return new SyncStatus("RUNNING", "starting", 0, 0, 0, 0, 0, 0, 0, 0, Instant.now(), null, null);
        }

        SyncStatus with(String newPhase, Function<SyncStatus, SyncStatus> change) {
            SyncStatus s = change.apply(this);
            return new SyncStatus(
                s.state,
                newPhase,
                s.categoriesCreated,
                s.categoriesUpdated,
                s.tagsCreated,
                s.tagsUpdated,
                s.postsCreated,
                s.postsUpdated,
                s.postsTotalInWordPress,
                s.postsOnlyInProject,
                s.startedAt,
                s.finishedAt,
                s.error
            );
        }

        SyncStatus withCategories(int created, int updated) {
            return new SyncStatus(
                state,
                phase,
                created,
                updated,
                tagsCreated,
                tagsUpdated,
                postsCreated,
                postsUpdated,
                postsTotalInWordPress,
                postsOnlyInProject,
                startedAt,
                finishedAt,
                error
            );
        }

        SyncStatus withTags(int created, int updated) {
            return new SyncStatus(
                state,
                phase,
                categoriesCreated,
                categoriesUpdated,
                created,
                updated,
                postsCreated,
                postsUpdated,
                postsTotalInWordPress,
                postsOnlyInProject,
                startedAt,
                finishedAt,
                error
            );
        }

        SyncStatus withPosts(int created, int updated, int total) {
            return new SyncStatus(
                state,
                phase,
                categoriesCreated,
                categoriesUpdated,
                tagsCreated,
                tagsUpdated,
                created,
                updated,
                total,
                postsOnlyInProject,
                startedAt,
                finishedAt,
                error
            );
        }

        SyncStatus withLocalOnly(long localOnly) {
            return new SyncStatus(
                state,
                phase,
                categoriesCreated,
                categoriesUpdated,
                tagsCreated,
                tagsUpdated,
                postsCreated,
                postsUpdated,
                postsTotalInWordPress,
                localOnly,
                startedAt,
                finishedAt,
                error
            );
        }

        SyncStatus finished(String failure) {
            return new SyncStatus(
                failure == null ? "DONE" : "FAILED",
                failure == null ? "done" : phase,
                categoriesCreated,
                categoriesUpdated,
                tagsCreated,
                tagsUpdated,
                postsCreated,
                postsUpdated,
                postsTotalInWordPress,
                postsOnlyInProject,
                startedAt,
                Instant.now(),
                failure
            );
        }
    }
}
