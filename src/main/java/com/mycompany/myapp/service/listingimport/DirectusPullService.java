package com.mycompany.myapp.service.listingimport;

import com.mycompany.myapp.config.ApplicationProperties;
import com.mycompany.myapp.security.SecurityUtils;
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
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.SerializationFeature;

/**
 * Bước 1 của đồng bộ API Trang Vàng: kéo doanh nghiệp đã xuất bản về {@code listing_source_record}, không đụng tới listing.
 * Đọc theo id tăng dần (không dùng số trang: bảng gần 2 triệu dòng), kéo lại thì cập nhật đúng dòng cũ. Ba cách kéo, xem {@link Mode}.
 * Bước so sánh và nhập ở {@link DirectusRecordSource}.
 */
@Service
public class DirectusPullService {

    public static final String RUNNING = "RUNNING";
    public static final String DONE = "DONE";
    public static final String FAILED = "FAILED";
    public static final String CANCELLED = "CANCELLED";
    /** Backend khởi động lại giữa chừng. */
    public static final String INTERRUPTED = "INTERRUPTED";

    private static final Logger LOG = LoggerFactory.getLogger(DirectusPullService.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final int DELAY_MS = 150;
    private static final int RECORD_COLUMNS = 10;
    private static final String FIELDS = "*,industry.industries_code";
    /** Lùi mốc "đã sửa sau" một ngày: source_last_updated của nguồn không ghi múi giờ. */
    private static final Duration SINCE_MARGIN = Duration.ofDays(1);
    private static final DateTimeFormatter SINCE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneOffset.UTC);

    /** Cách kéo. */
    public enum Mode {
        /**
         * Chỉ phần thay đổi (mặc định): id lớn hơn id lớn nhất đã lưu, cộng bản ghi có {@code date_updated} hoặc
         * {@code source_last_updated} sau lần kéo toàn bộ gần nhất. Bản ghi bị sửa mà nguồn không ghi ngày thì không thấy.
         */
        NEW,
        /** Kéo lại cả bảng (khoảng 25 phút), bắt được mọi thay đổi. Nên chạy định kỳ. */
        FULL,
        /** Đọc tiếp từ id lớn nhất đã lưu, khi lần kéo trước bị lỗi giữa chừng. */
        RESUME,
    }

    /** Một lần lấy dữ liệu (bảng listing_source_pull). */
    public record PullStatus(
        Long id,
        String state,
        long read,
        long changed,
        long lastId,
        Instant startedAt,
        Instant finishedAt,
        String error,
        Mode mode
    ) {}

    /** Tình trạng bảng bản gốc: đã lưu bao nhiêu, bao nhiêu mới hoặc đã đổi so với lần nhập thật gần nhất. */
    /** @param unfinishedFull lần lấy toàn bộ gần nhất nếu nó bị dừng giữa chừng (lấy tiếp được); null nếu không có */
    public record Summary(PullStatus pull, long stored, long pending, Instant lastFetchedAt, PullStatus unfinishedFull) {}

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
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);
    /** Số liệu bảng lúc bắt đầu lần kéo đang chạy. */
    private volatile Summary baseline;
    /** Lần đếm bảng gần nhất (màn hình đếm khi mở trang): bấm lấy dữ liệu không phải đếm lại 1,4 triệu dòng. */
    private volatile Summary lastCounted;

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

    /** Lần lấy còn RUNNING trong DB lúc khởi động là lần bị cắt ngang khi backend tắt. */
    @EventListener(ApplicationReadyEvent.class)
    void markInterrupted() {
        try {
            // CSDL đặt auto-commit = false: mọi câu ghi phải nằm trong transaction, không thì bị hủy khi trả kết nối
            Integer n = tx.execute(s ->
                jdbc.update(
                    "update listing_source_pull set status = ?, finished_at = ? where status = ?",
                    INTERRUPTED,
                    Timestamp.from(Instant.now()),
                    RUNNING
                )
            );
            if (n != null && n > 0) {
                LOG.warn("{} lần lấy dữ liệu API Trang Vàng bị gián đoạn khi backend khởi động lại", n);
            }
        } catch (DataAccessException e) {
            LOG.warn("Chưa đọc được listing_source_pull (Liquibase chưa chạy xong?): {}", e.getMessage());
        }
    }

    /** Dừng lần lấy đang chạy sau trang đang đọc; phần đã lưu vẫn giữ. */
    public void cancel() {
        if (!running.get()) {
            throw new IllegalStateException("Không có lần lấy dữ liệu nào đang chạy");
        }
        cancelRequested.set(true);
    }

    /** Lần lấy gần nhất; null nếu chưa lấy lần nào. */
    PullStatus lastRun() {
        return latest("");
    }

    /** Lần lấy toàn bộ gần nhất nếu nó dừng giữa chừng (lỗi, hủy, gián đoạn). Lấy bản mới sau đó không làm mất nút Lấy tiếp. */
    PullStatus unfinishedFull() {
        PullStatus full = latest(" and mode = 'FULL'");
        return full == null || RUNNING.equals(full.state()) || DONE.equals(full.state()) ? null : full;
    }

    private PullStatus latest(String where) {
        List<PullStatus> rows = jdbc.query(
            "select * from listing_source_pull where source = ?" + where + " order by id desc limit 1",
            (rs, n) ->
                new PullStatus(
                    rs.getLong("id"),
                    rs.getString("status"),
                    rs.getLong("read_count"),
                    rs.getLong("changed_count"),
                    rs.getLong("last_id"),
                    rs.getTimestamp("started_at").toInstant(),
                    rs.getTimestamp("finished_at") == null ? null : rs.getTimestamp("finished_at").toInstant(),
                    rs.getString("error"),
                    Mode.valueOf(rs.getString("mode"))
                ),
            DirectusRecordSource.SOURCE
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** Bắt đầu kéo ở nền. */
    public Summary start(Mode mode) {
        if (config.getToken() == null || config.getToken().isBlank()) {
            throw new IllegalArgumentException("Chưa khai báo token API Trang Vàng (biến môi trường YP_DIRECTUS_TOKEN)");
        }
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Đang kéo dữ liệu từ API");
        }
        try {
            // Chỉ làm việc nhanh ở đây để nút phản hồi ngay; id bắt đầu và mốc "đã sửa sau" tính ở luồng nền
            long cursor = 0;
            if (mode == Mode.RESUME) {
                PullStatus last = unfinishedFull();
                if (last == null) {
                    throw new IllegalArgumentException("Không có lần lấy toàn bộ nào bị dừng giữa chừng để lấy tiếp");
                }
                cursor = last.lastId();
            }
            // Lấy tiếp là tiếp tục lần lấy toàn bộ: lưu là FULL để nếu lại dừng thì vẫn lấy tiếp được
            Mode saved = mode == Mode.RESUME ? Mode.FULL : mode;
            Summary known = lastCounted;
            baseline = known != null ? known : count();
            Instant now = Instant.now();
            long startId = cursor;
            GeneratedKeyHolder key = new GeneratedKeyHolder();
            tx.executeWithoutResult(s ->
                jdbc.update(con -> {
                    PreparedStatement ps = con.prepareStatement(
                        "insert into listing_source_pull (source, mode, status, read_count, changed_count, last_id, started_at, started_by) " +
                            "values (?, ?, ?, 0, 0, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                    );
                    ps.setString(1, DirectusRecordSource.SOURCE);
                    ps.setString(2, saved.name());
                    ps.setString(3, RUNNING);
                    ps.setLong(4, startId);
                    ps.setTimestamp(5, Timestamp.from(now));
                    ps.setString(6, SecurityUtils.getCurrentUserLogin().orElse(null));
                    return ps;
                }, key)
            );
            status.set(new PullStatus(key.getKey().longValue(), RUNNING, 0, 0, cursor, now, null, null, saved));
            cancelRequested.set(false);
            executor.submit(() -> run(mode, startId));
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
            return new Summary(now, base.stored() + now.changed(), base.pending() + now.changed(), base.lastFetchedAt(), null);
        }
        Summary counted = count();
        return new Summary(lastRun(), counted.stored(), counted.pending(), counted.lastFetchedAt(), unfinishedFull());
    }

    private Summary count() {
        Summary counted = jdbc.queryForObject(
            "select count(*) stored_count, coalesce(sum(imported_hash is null or imported_hash <> content_hash), 0) pending_count, " +
                "max(last_fetched_at) last_fetched_at from listing_source_record where source = ?",
            (rs, n) -> {
                Timestamp last = rs.getTimestamp("last_fetched_at");
                return new Summary(
                    null,
                    rs.getLong("stored_count"),
                    rs.getLong("pending_count"),
                    last == null ? null : last.toInstant(),
                    null
                );
            },
            DirectusRecordSource.SOURCE
        );
        lastCounted = counted;
        return counted;
    }

    private long maxStoredId() {
        Long max = jdbc.queryForObject(
            "select coalesce(max(cast(source_id as unsigned)), 0) from listing_source_record where source = ?",
            Long.class,
            DirectusRecordSource.SOURCE
        );
        return max == null ? 0 : max;
    }

    /**
     * Mốc "đã sửa sau": lúc bắt đầu lần lấy toàn bộ gần nhất đã xong (bảng listing_source_pull, tức thì), lùi một ngày.
     * Chưa có lần nào ghi ở đó thì lấy lần lấy sớm nhất của bảng bản gốc (quét index, khoảng 1 giây). Bảng trống thì null.
     */
    private String changedSince() {
        Timestamp mark = jdbc.queryForObject(
            "select max(started_at) from listing_source_pull where source = ? and mode = 'FULL' and status = 'DONE'",
            Timestamp.class,
            DirectusRecordSource.SOURCE
        );
        if (mark == null) {
            mark = jdbc.queryForObject(
                "select min(last_fetched_at) from listing_source_record where source = ?",
                Timestamp.class,
                DirectusRecordSource.SOURCE
            );
        }
        return mark == null ? null : SINCE_FORMAT.format(mark.toInstant().minus(SINCE_MARGIN));
    }

    /**
     * Kéo phần thay đổi: lượt 1 bản ghi đã sửa sau {@code since} (nếu có), lượt 2 bản ghi có id mới. Kéo toàn bộ hay đọc tiếp: chỉ lượt 2.
     */
    private void run(Mode mode, long resumeFrom) {
        long[] progress = { 0, 0, resumeFrom }; // đã đọc, mới hoặc đã đổi, id cuối
        try {
            long from = switch (mode) {
                case RESUME -> resumeFrom;
                case FULL -> 0;
                case NEW -> maxStoredId();
            };
            String since = mode == Mode.NEW ? changedSince() : null;
            if (since != null) {
                walk(0, since, progress);
            }
            walk(from, null, progress);
            if (Thread.currentThread().isInterrupted()) {
                update(INTERRUPTED, progress, null);
            } else {
                update(cancelRequested.get() ? CANCELLED : DONE, progress, null);
            }
        } catch (RuntimeException e) {
            LOG.error("Kéo dữ liệu API Trang Vàng lỗi sau id {}", progress[2], e);
            update(FAILED, progress, e.getMessage());
        } finally {
            running.set(false);
        }
    }

    private void walk(long from, String since, long[] progress) {
        long cursor = from;
        while (!Thread.currentThread().isInterrupted() && !cancelRequested.get()) {
            List<JsonNode> items = fetch(cursor, since);
            if (items.isEmpty()) {
                return;
            }
            progress[1] += save(items);
            progress[0] += items.size();
            cursor = items
                .get(items.size() - 1)
                .path("id")
                .asLong();
            progress[2] = cursor;
            update(RUNNING, progress, null);
        }
    }

    private void update(String state, long[] progress, String error) {
        PullStatus old = status.get();
        PullStatus now = new PullStatus(
            old.id(),
            state,
            progress[0],
            progress[1],
            progress[2],
            old.startedAt(),
            RUNNING.equals(state) ? null : Instant.now(),
            error,
            old.mode()
        );
        status.set(now);
        try {
            tx.executeWithoutResult(s ->
                jdbc.update(
                    "update listing_source_pull set status = ?, read_count = ?, changed_count = ?, last_id = ?, finished_at = ?, error = ? where id = ?",
                    now.state(),
                    now.read(),
                    now.changed(),
                    now.lastId(),
                    now.finishedAt() == null ? null : Timestamp.from(now.finishedAt()),
                    ListingImportNormalizer.cut(error, 1000),
                    now.id()
                )
            );
        } catch (DataAccessException e) {
            // Backend đang tắt: lần khởi động sau sẽ đánh dấu gián đoạn
            LOG.warn("Không ghi được tiến độ lấy dữ liệu: {}", e.getMessage());
        }
    }

    /** Bộ lọc Directus: đã xuất bản, id sau con trỏ, và nếu có {@code since} thì đã sửa sau mốc đó. */
    static String filter(long cursor, String since) {
        String base = "{\"status\":{\"_eq\":\"published\"}},{\"id\":{\"_gt\":" + cursor + "}}";
        if (since != null) {
            base += ",{\"_or\":[{\"date_updated\":{\"_gt\":\"" + since + "\"}},{\"source_last_updated\":{\"_gt\":\"" + since + "\"}}]}";
        }
        return "{\"_and\":[" + base + "]}";
    }

    private List<JsonNode> fetch(long cursor, String since) {
        String url =
            config.getBaseUrl().replaceAll("/+$", "") +
            "/items/listings?limit=" +
            config.getPageSize() +
            "&sort=id&filter=" +
            URLEncoder.encode(filter(cursor, since), StandardCharsets.UTF_8) +
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
