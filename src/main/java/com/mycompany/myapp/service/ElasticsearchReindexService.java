package com.mycompany.myapp.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.mycompany.myapp.domain.BlogCategory;
import com.mycompany.myapp.domain.BlogPost;
import com.mycompany.myapp.domain.Category;
import com.mycompany.myapp.domain.Listing;
import com.mycompany.myapp.domain.Location;
import com.mycompany.myapp.domain.Tag;
import com.mycompany.myapp.domain.User;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Đưa toàn bộ dữ liệu từ MySQL vào Elasticsearch (reindex).
 * <p>
 * JHipster chỉ ghi vào Elasticsearch khi lưu qua service; dữ liệu chép thẳng vào MySQL (db-sync) thì ES không biết,
 * nên cần job này. Mỗi entity: tạo lại index với mapping từ annotation, đọc DB theo lô (keyset theo id),
 * ghi ES bằng bulk, rồi bật lại refresh. Chạy trên một luồng riêng, chỉ một job tại một thời điểm.
 */
@Service
public class ElasticsearchReindexService {

    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchReindexService.class);

    /** Số dòng mỗi lô đọc DB / ghi bulk. */
    static final int BATCH_SIZE = 1000;

    /** Tên dùng trong API -> entity. Thứ tự là thứ tự reindex (bảng nhỏ trước). */
    static final Map<String, Class<?>> ENTITIES;

    static {
        Map<String, Class<?>> entities = new LinkedHashMap<>();
        entities.put("category", Category.class);
        entities.put("location", Location.class);
        entities.put("tag", Tag.class);
        entities.put("blogcategory", BlogCategory.class);
        entities.put("blogpost", BlogPost.class);
        entities.put("user", User.class);
        entities.put("listing", Listing.class);
        ENTITIES = java.util.Collections.unmodifiableMap(entities);
    }

    private final EntityManager entityManager;
    private final TransactionTemplate readOnlyTransaction;
    private final ElasticsearchTemplate elasticsearchTemplate;
    private final ElasticsearchClient elasticsearchClient;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "elasticsearch-reindex");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile ReindexStatus status = ReindexStatus.idle();

    public ElasticsearchReindexService(
        EntityManager entityManager,
        PlatformTransactionManager transactionManager,
        ElasticsearchTemplate elasticsearchTemplate,
        ElasticsearchClient elasticsearchClient
    ) {
        this.entityManager = entityManager;
        this.readOnlyTransaction = new TransactionTemplate(transactionManager);
        this.readOnlyTransaction.setReadOnly(true);
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.elasticsearchClient = elasticsearchClient;
    }

    /**
     * Bắt đầu reindex ở chế độ nền.
     *
     * @param names tên entity (xem {@link #ENTITIES}); rỗng hoặc null = tất cả.
     * @return trạng thái ngay sau khi bắt đầu.
     * @throws IllegalArgumentException nếu có tên không hợp lệ.
     * @throws IllegalStateException nếu đang có job chạy.
     */
    public ReindexStatus start(Collection<String> names) {
        List<String> selected = names == null || names.isEmpty() ? List.copyOf(ENTITIES.keySet()) : List.copyOf(names);
        List<String> unknown = selected
            .stream()
            .filter(name -> !ENTITIES.containsKey(name))
            .toList();
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("Entity không hợp lệ: " + unknown + ". Hợp lệ: " + ENTITIES.keySet());
        }
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Đang có job reindex chạy");
        }
        // Giữ thứ tự của ENTITIES để bảng nhỏ chạy trước
        List<String> ordered = ENTITIES.keySet().stream().filter(selected::contains).toList();
        status = ReindexStatus.started(ordered);
        executor.submit(() -> run(ordered));
        return status;
    }

    public ReindexStatus getStatus() {
        return status;
    }

    /**
     * So số dòng MySQL với số tài liệu Elasticsearch của từng entity, để biết index đã đủ chưa.
     * {@code esCount = -1} nghĩa là index chưa tồn tại.
     */
    public List<IndexCheck> checkIndices() {
        return ENTITIES.entrySet()
            .stream()
            .map(entry -> {
                Class<?> type = entry.getValue();
                long dbCount = readOnlyTransaction.execute(tx ->
                    entityManager.createQuery("select count(e) from " + type.getSimpleName() + " e", Long.class).getSingleResult()
                );
                IndexOperations indexOps = elasticsearchTemplate.indexOps(type);
                String indexName = indexOps.getIndexCoordinates().getIndexName();
                long esCount = -1;
                if (indexOps.exists()) {
                    // Refresh trước khi đếm để không lệch vì tài liệu vừa ghi chưa hiện
                    indexOps.refresh();
                    esCount = elasticsearchTemplate.count(Query.findAll(), type);
                }
                return new IndexCheck(entry.getKey(), indexName, dbCount, esCount, dbCount == esCount);
            })
            .toList();
    }

    /** Kết quả so sánh MySQL và Elasticsearch của một entity. */
    public record IndexCheck(String entity, String index, long dbCount, long esCount, boolean complete) {}

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    /**
     * Job bị ngắt giữa chừng (tắt app, app chết) sẽ để index kẹt ở refresh_interval=-1: tài liệu mới ghi qua service
     * không hiện trong tìm kiếm. Khi app khởi động, đặt lại 1s cho các index đó.
     */
    @EventListener(ApplicationReadyEvent.class)
    void restoreRefreshAfterInterruptedReindex() {
        for (Class<?> type : ENTITIES.values()) {
            String indexName = elasticsearchTemplate.indexOps(type).getIndexCoordinates().getIndexName();
            try {
                var settings = elasticsearchClient
                    .indices()
                    .getSettings(s -> s.index(indexName))
                    .get(indexName);
                var refresh =
                    settings == null || settings.settings() == null || settings.settings().index() == null
                        ? null
                        : settings.settings().index().refreshInterval();
                if (refresh != null && "-1".equals(refresh.time())) {
                    LOG.warn("Index {} còn refresh_interval=-1 do lần reindex trước bị ngắt; đặt lại 1s. Nên chạy lại reindex.", indexName);
                    setRefreshInterval(indexName, "1s");
                }
            } catch (Exception e) {
                // Index chưa tồn tại hoặc ES chưa sẵn sàng: bỏ qua, không chặn app khởi động
                LOG.debug("Không kiểm tra được refresh_interval của {}: {}", indexName, e.getMessage());
            }
        }
    }

    private void run(List<String> names) {
        try {
            for (String name : names) {
                reindex(name, ENTITIES.get(name));
            }
            status = status.finished(null);
            LOG.info("Reindex xong: {}", status);
        } catch (Exception e) {
            LOG.error("Reindex lỗi", e);
            status = status.finished(e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            running.set(false);
        }
    }

    private void reindex(String name, Class<?> type) throws IOException {
        Instant start = Instant.now();
        long total = readOnlyTransaction.execute(tx ->
            entityManager.createQuery("select count(e) from " + type.getSimpleName() + " e", Long.class).getSingleResult()
        );
        status = status.progress(name, 0, total);
        LOG.info("Reindex {}: {} dòng", name, total);

        IndexOperations indexOps = elasticsearchTemplate.indexOps(type);
        indexOps.delete();
        indexOps.createWithMapping();
        String indexName = indexOps.getIndexCoordinates().getIndexName();
        setRefreshInterval(indexName, "-1");
        try {
            long done = 0;
            Long lastId = 0L;
            while (true) {
                final Long after = lastId;
                List<?> batch = readOnlyTransaction.execute(tx -> {
                    List<?> rows = entityManager
                        .createQuery("select e from " + type.getSimpleName() + " e where e.id > :after order by e.id", type)
                        .setParameter("after", after)
                        .setMaxResults(BATCH_SIZE)
                        .getResultList();
                    // Ghi ES trong transaction để field lazy (nếu có) vẫn đọc được, rồi giải phóng bộ nhớ Hibernate
                    if (!rows.isEmpty()) {
                        elasticsearchTemplate.save(new ArrayList<>(rows));
                    }
                    List<Object> ids = rows
                        .stream()
                        .map(entityManager.getEntityManagerFactory().getPersistenceUnitUtil()::getIdentifier)
                        .toList();
                    entityManager.clear();
                    return ids;
                });
                if (batch == null || batch.isEmpty()) {
                    break;
                }
                lastId = (Long) batch.get(batch.size() - 1);
                done += batch.size();
                status = status.progress(name, done, total);
                if (done % (BATCH_SIZE * 50L) == 0) {
                    LOG.info("Reindex {}: {}/{}", name, done, total);
                }
            }
        } finally {
            setRefreshInterval(indexName, "1s");
        }
        indexOps.refresh();
        LOG.info("Reindex {} xong trong {}", name, Duration.between(start, Instant.now()));
    }

    private void setRefreshInterval(String indexName, String interval) throws IOException {
        elasticsearchClient.indices().putSettings(s -> s.index(indexName).settings(i -> i.refreshInterval(t -> t.time(interval))));
    }

    /** Trạng thái job (bất biến, thay cả object khi cập nhật). */
    public record ReindexStatus(
        String state,
        List<String> entities,
        String currentEntity,
        Map<String, Progress> progress,
        Instant startedAt,
        Instant finishedAt,
        String error
    ) {
        public record Progress(long done, long total) {}

        static ReindexStatus idle() {
            return new ReindexStatus("IDLE", List.of(), null, Map.of(), null, null, null);
        }

        static ReindexStatus started(List<String> entities) {
            return new ReindexStatus("RUNNING", entities, null, Map.of(), Instant.now(), null, null);
        }

        ReindexStatus progress(String entity, long done, long total) {
            Map<String, Progress> next = new LinkedHashMap<>(progress);
            next.put(entity, new Progress(done, total));
            return new ReindexStatus(state, entities, entity, java.util.Collections.unmodifiableMap(next), startedAt, finishedAt, error);
        }

        ReindexStatus finished(String failure) {
            return new ReindexStatus(failure == null ? "DONE" : "FAILED", entities, null, progress, startedAt, Instant.now(), failure);
        }
    }
}
