package com.mycompany.myapp.service.listingimport;

import com.mycompany.myapp.config.ApplicationProperties;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.SerializationFeature;

/**
 * Bước 1 của đồng bộ API Trang Vàng: kéo doanh nghiệp đã xuất bản về {@code listing_source_record}, không đụng tới listing.
 * Đọc theo id tăng dần (không dùng số trang: bảng gần 2 triệu dòng), kéo lại thì cập nhật đúng dòng cũ. Đứt giữa chừng thì
 * chạy tiếp từ id lớn nhất đã lưu. Bước so sánh và nhập ở {@link DirectusRecordSource}.
 */
@Service
public class DirectusPullService {

    public static final String RUNNING = "RUNNING";
    public static final String DONE = "DONE";
    public static final String FAILED = "FAILED";

    private static final Logger LOG = LoggerFactory.getLogger(DirectusPullService.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final int DELAY_MS = 150;
    private static final int RECORD_COLUMNS = 10;
    private static final String FIELDS = "*,industry.industries_code";

    /** Tiến độ lần kéo gần nhất từ khi backend khởi động. */
    public record PullStatus(String state, long read, long changed, long lastId, Instant startedAt, Instant finishedAt, String error) {}

    /** Tình trạng bảng bản gốc: đã lưu bao nhiêu, bao nhiêu mới hoặc đã đổi so với lần nhập thật gần nhất. */
    public record Summary(PullStatus pull, long stored, long pending, Instant lastFetchedAt) {}

    private final ApplicationProperties.Directus config;
    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;
    private final TransactionTemplate tx;
    private final ObjectMapper objectMapper;
    /** JSON gọn một dòng (ObjectMapper của profile dev bật INDENT_OUTPUT). */
    private final ObjectWriter compactJson;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "directus-pull");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<PullStatus> status = new AtomicReference<>();
    /** Số liệu bảng lúc bắt đầu lần kéo đang chạy. */
    private volatile Summary baseline;

    public DirectusPullService(
        ApplicationProperties applicationProperties,
        DataSource dataSource,
        PlatformTransactionManager transactionManager,
        ObjectMapper objectMapper
    ) {
        this.config = applicationProperties.getDirectus();
        this.jdbc = new JdbcTemplate(dataSource);
        this.named = new NamedParameterJdbcTemplate(dataSource);
        this.tx = new TransactionTemplate(transactionManager);
        this.objectMapper = objectMapper;
        this.compactJson = objectMapper.writer().without(SerializationFeature.INDENT_OUTPUT);
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    public boolean isRunning() {
        return running.get();
    }

    /** Bắt đầu kéo ở nền. {@code resume}: đọc tiếp từ id lớn nhất đã lưu thay vì từ đầu. */
    public Summary start(boolean resume) {
        if (config.getToken() == null || config.getToken().isBlank()) {
            throw new IllegalArgumentException("Chưa khai báo token API Trang Vàng (biến môi trường YP_DIRECTUS_TOKEN)");
        }
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Đang kéo dữ liệu từ API");
        }
        try {
            long cursor = 0;
            if (resume) {
                Long max = jdbc.queryForObject(
                    "select coalesce(max(cast(source_id as unsigned)), 0) from listing_source_record where source = ?",
                    Long.class,
                    DirectusRecordSource.SOURCE
                );
                cursor = max == null ? 0 : max;
            }
            baseline = count();
            status.set(new PullStatus(RUNNING, 0, 0, cursor, Instant.now(), null, null));
            long from = cursor;
            executor.submit(() -> run(from));
        } catch (RuntimeException e) {
            running.set(false);
            throw e;
        }
        return summary();
    }

    /**
     * Tình trạng bảng bản gốc. Đang kéo thì tính từ số đầu đợt, không đếm lại bảng: màn hình hỏi mỗi 2 giây, đếm lại liên tục
     * từng làm cạn kết nối CSDL.
     */
    public Summary summary() {
        Summary base = baseline;
        if (running.get() && base != null) {
            PullStatus now = status.get();
            return new Summary(now, base.stored() + now.changed(), base.pending() + now.changed(), base.lastFetchedAt());
        }
        return count();
    }

    private Summary count() {
        return jdbc.queryForObject(
            "select count(*) stored_count, coalesce(sum(imported_hash is null or imported_hash <> content_hash), 0) pending_count, " +
                "max(last_fetched_at) last_fetched_at from listing_source_record where source = ?",
            (rs, n) -> {
                Timestamp last = rs.getTimestamp("last_fetched_at");
                return new Summary(
                    status.get(),
                    rs.getLong("stored_count"),
                    rs.getLong("pending_count"),
                    last == null ? null : last.toInstant()
                );
            },
            DirectusRecordSource.SOURCE
        );
    }

    private void run(long from) {
        long cursor = from;
        long read = 0;
        long changed = 0;
        try {
            while (!Thread.currentThread().isInterrupted()) {
                List<JsonNode> items = fetch(cursor);
                if (items.isEmpty()) {
                    break;
                }
                changed += save(items);
                read += items.size();
                cursor = items
                    .get(items.size() - 1)
                    .path("id")
                    .asLong();
                update(RUNNING, read, changed, cursor, null);
            }
            update(DONE, read, changed, cursor, null);
        } catch (RuntimeException e) {
            LOG.error("Kéo dữ liệu API Trang Vàng lỗi sau id {}", cursor, e);
            update(FAILED, read, changed, cursor, e.getMessage());
        } finally {
            running.set(false);
        }
    }

    private void update(String state, long read, long changed, long lastId, String error) {
        PullStatus old = status.get();
        status.set(new PullStatus(state, read, changed, lastId, old.startedAt(), RUNNING.equals(state) ? null : Instant.now(), error));
    }

    private List<JsonNode> fetch(long cursor) {
        String url =
            config.getBaseUrl().replaceAll("/+$", "") +
            "/items/listings?limit=" +
            config.getPageSize() +
            "&sort=id&filter%5Bstatus%5D=published&filter%5Bid%5D%5B_gt%5D=" +
            cursor +
            "&fields=" +
            URLEncoder.encode(FIELDS, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(120))
            .header("Accept", "application/json")
            .header("Authorization", "Bearer " + config.getToken())
            .GET()
            .build();
        for (int attempt = 1; ; attempt++) {
            try {
                Thread.sleep(attempt == 1 ? DELAY_MS : 5_000L * attempt);
                HttpResponse<String> res = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (res.statusCode() / 100 == 2) {
                    List<JsonNode> items = new ArrayList<>();
                    objectMapper.readTree(res.body()).path("data").forEach(items::add);
                    return items;
                }
                if (res.statusCode() < 500 || attempt == MAX_ATTEMPTS) {
                    throw new IllegalStateException("API Trang Vàng trả HTTP " + res.statusCode() + " sau id " + cursor);
                }
            } catch (IOException e) {
                if (attempt == MAX_ATTEMPTS) {
                    throw new IllegalStateException("Không gọi được API Trang Vàng sau id " + cursor, e);
                }
                LOG.warn("API Trang Vàng lỗi lần {} sau id {}: {}", attempt, cursor, e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return List.of();
            }
        }
    }

    /** Ghi một trang vào bảng bản gốc; trả số bản ghi mới hoặc đã đổi. */
    private long save(List<JsonNode> items) {
        Map<String, String> payloads = new HashMap<>();
        items.forEach(item -> payloads.put(item.path("id").asString(), compactJson.writeValueAsString(item)));
        Map<String, String> oldHashes = new HashMap<>();
        named.query(
            "select source_id, content_hash from listing_source_record where source = :source and source_id in (:ids)",
            new MapSqlParameterSource("source", DirectusRecordSource.SOURCE).addValue("ids", payloads.keySet()),
            rs -> {
                oldHashes.put(rs.getString("source_id"), rs.getString("content_hash"));
            }
        );

        Timestamp now = Timestamp.from(Instant.now());
        List<Object> args = new ArrayList<>(items.size() * RECORD_COLUMNS);
        long changed = 0;
        for (JsonNode item : items) {
            String sourceId = item.path("id").asString();
            String payload = payloads.get(sourceId);
            String hash = sha256(payload);
            if (!hash.equals(oldHashes.get(sourceId))) {
                changed++;
            }
            args.add(DirectusRecordSource.SOURCE);
            args.add(sourceId);
            args.add(ListingImportNormalizer.cut(ApiRecordSource.text(item.path("taxcode")), 100));
            args.add(ListingImportNormalizer.cut(ApiRecordSource.text(item.path("slug")), 500));
            args.add(ListingImportNormalizer.cut(ApiRecordSource.text(item.path("status")), 30));
            args.add(payload);
            args.add(hash);
            args.add(sourceTime(ApiRecordSource.text(item.path("source_last_updated"))));
            args.add(now);
            args.add(now);
        }
        String row = "(" + "?,".repeat(RECORD_COLUMNS - 1) + "?)";
        String sql =
            "insert into listing_source_record " +
            "(source, source_id, tax_code, slug, status, payload, content_hash, source_updated_at, first_fetched_at, last_fetched_at) " +
            "values " +
            String.join(",", Collections.nCopies(items.size(), row)) +
            " as n on duplicate key update tax_code = n.tax_code, slug = n.slug, status = n.status, payload = n.payload, " +
            "content_hash = n.content_hash, source_updated_at = n.source_updated_at, last_fetched_at = n.last_fetched_at";
        tx.executeWithoutResult(s -> jdbc.update(sql, args.toArray()));
        return changed;
    }

    /** "2026-03-31T11:52:03" → thời điểm; không đọc được thì null. */
    static Timestamp sourceTime(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Timestamp.from(LocalDateTime.parse(value.substring(0, Math.min(19, value.length()))).toInstant(ZoneOffset.UTC));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
