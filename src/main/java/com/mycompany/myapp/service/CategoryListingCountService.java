package com.mycompany.myapp.service;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Tính lại {@code category.listing_count}: số doanh nghiệp đã xuất bản của mỗi ngành, <b>tính cả ngành con</b>,
 * mỗi doanh nghiệp đếm một lần (doanh nghiệp gần như chỉ gắn vào ngành lá).
 * <p>
 * Dữ liệu gốc ({@code jhipster_vnyp}) không có số này (cột bằng 0), và đếm trực tiếp trên ~25 triệu liên kết mỗi lần
 * FE gọi thì quá chậm, nên tính một lần rồi lưu. Đọc tuần tự bảng {@code rel_listing__category} theo khóa chính
 * (streaming JDBC, không nạp hết vào bộ nhớ), đếm bằng {@link CategorySubtreeCounter}, rồi chỉ cập nhật dòng thay đổi.
 * Chạy nền trên một luồng riêng, mỗi lúc một job. Cần chạy lại sau khi đồng bộ dữ liệu hoặc thay đổi nhiều liên kết.
 */
@Service
public class CategoryListingCountService {

    private static final Logger LOG = LoggerFactory.getLogger(CategoryListingCountService.class);

    static final String PUBLISHED = "publish";

    private final JdbcTemplate jdbcTemplate;
    /** JdbcTemplate riêng cho streaming: fetchSize = Integer.MIN_VALUE là cách MySQL Connector/J đọc từng dòng. */
    private final JdbcTemplate streamingJdbcTemplate;
    private final ElasticsearchReindexService reindexService;
    /** Hikari đặt auto-commit: false, nên UPDATE phải chạy trong transaction, nếu không sẽ bị rollback khi trả kết nối. */
    private final TransactionTemplate transaction;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "category-listing-count");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong rowsRead = new AtomicLong();
    private volatile Status status = Status.idle();

    public CategoryListingCountService(
        DataSource dataSource,
        PlatformTransactionManager transactionManager,
        ElasticsearchReindexService reindexService
    ) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.streamingJdbcTemplate = new JdbcTemplate(dataSource);
        this.streamingJdbcTemplate.setFetchSize(Integer.MIN_VALUE);
        this.reindexService = reindexService;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * Bắt đầu tính lại ở chế độ nền.
     *
     * @throws IllegalStateException nếu đang có job chạy.
     */
    public Status start() {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Đang tính số doanh nghiệp theo ngành");
        }
        rowsRead.set(0);
        status = Status.started();
        executor.submit(this::run);
        return status;
    }

    public Status getStatus() {
        Status current = status;
        return "RUNNING".equals(current.state()) ? current.withRowsRead(rowsRead.get()) : current;
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    private void run() {
        Instant start = Instant.now();
        try {
            Map<Long, Long> parentById = new HashMap<>();
            jdbcTemplate.query("select id, parent_id from category", rs -> {
                parentById.put(rs.getLong("id"), rs.getObject("parent_id", Long.class));
            });
            Set<Long> excluded = new HashSet<>(
                jdbcTemplate.queryForList("select id from listing where status is null or status <> ?", Long.class, PUBLISHED)
            );
            LOG.info("Đếm doanh nghiệp theo ngành: {} ngành, bỏ qua {} listing chưa xuất bản", parentById.size(), excluded.size());

            CategorySubtreeCounter counter = new CategorySubtreeCounter(parentById, excluded);
            streamingJdbcTemplate.query(
                "select listing_id, category_id from rel_listing__category order by listing_id, category_id",
                rs -> {
                    counter.accept(rs.getLong(1), rs.getLong(2));
                    long read = rowsRead.incrementAndGet();
                    if (read % 5_000_000 == 0) {
                        LOG.info("Đếm doanh nghiệp theo ngành: đã đọc {} liên kết", read);
                    }
                }
            );

            Map<Long, Long> counts = counter.result();
            List<Object[]> updates = new ArrayList<>();
            counts.forEach((id, count) -> updates.add(new Object[] { count, id, count }));
            int[][] batches = transaction.execute(tx ->
                jdbcTemplate.batchUpdate(
                    "update category set listing_count = ? where id = ? and (listing_count is null or listing_count <> ?)",
                    updates,
                    500,
                    (ps, row) -> {
                        ps.setLong(1, (Long) row[0]);
                        ps.setLong(2, (Long) row[1]);
                        ps.setLong(3, (Long) row[2]);
                    }
                )
            );
            int changed = 0;
            for (int[] batch : batches == null ? new int[0][] : batches) {
                for (int n : batch) {
                    changed += Math.max(n, 0);
                }
            }
            long nonEmpty = counts
                .values()
                .stream()
                .filter(c -> c > 0)
                .count();
            Duration took = Duration.between(start, Instant.now());
            LOG.info(
                "Đếm doanh nghiệp theo ngành xong trong {}: {} liên kết, {} ngành có doanh nghiệp, cập nhật {} dòng",
                took,
                rowsRead.get(),
                nonEmpty,
                changed
            );
            status = status.finished(rowsRead.get(), counts.size(), nonEmpty, changed, null);
            reindexCategories();
        } catch (Exception e) {
            LOG.error("Đếm doanh nghiệp theo ngành lỗi", e);
            status = status.finished(rowsRead.get(), 0, 0, 0, e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            running.set(false);
        }
    }

    /** listingCount cũng nằm trong index category của ES: reindex (2.394 dòng, vài giây). Đang có job reindex khác thì bỏ qua. */
    private void reindexCategories() {
        try {
            reindexService.start(List.of("category"));
        } catch (IllegalStateException e) {
            LOG.warn("Không reindex được category vì đang có job reindex khác; chạy lại sau: reindex?entities=category");
        }
    }

    /**
     * Trạng thái job (bất biến).
     *
     * @param categories        số ngành.
     * @param nonEmptyCategories số ngành có ít nhất một doanh nghiệp.
     * @param updatedRows       số dòng {@code category} có giá trị thay đổi.
     */
    public record Status(
        String state,
        long rowsRead,
        long categories,
        long nonEmptyCategories,
        long updatedRows,
        Instant startedAt,
        Instant finishedAt,
        String error
    ) {
        static Status idle() {
            return new Status("IDLE", 0, 0, 0, 0, null, null, null);
        }

        static Status started() {
            return new Status("RUNNING", 0, 0, 0, 0, Instant.now(), null, null);
        }

        Status withRowsRead(long read) {
            return new Status(state, read, categories, nonEmptyCategories, updatedRows, startedAt, finishedAt, error);
        }

        Status finished(long read, long total, long nonEmpty, long updated, String failure) {
            return new Status(failure == null ? "DONE" : "FAILED", read, total, nonEmpty, updated, startedAt, Instant.now(), failure);
        }
    }
}
