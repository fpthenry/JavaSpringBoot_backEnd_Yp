package com.mycompany.myapp.service.dvhc;

import com.mycompany.myapp.service.dvhc.ListingLinkPlanner.Context;
import com.mycompany.myapp.service.dvhc.ListingLinkPlanner.Decision;
import com.mycompany.myapp.service.dvhc.ListingLinkPlanner.Unit;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * Gắn doanh nghiệp vào đơn vị hành chính mới (và đơn vị cũ khi còn thiếu), theo {@link ListingLinkPlanner}.
 * Chỉ <b>thêm</b> dòng vào {@code rel_listing__location} ({@code INSERT IGNORE}), không xóa liên kết nào; chạy lại được.
 * <p>
 * Nạp mọi liên kết hiện có vào mảng số (~1,8 triệu cặp, vài chục MB), đọc tuần tự {@code listing} theo id bằng JDBC
 * streaming, ghi theo lô (mỗi lệnh 1.000 dòng, mỗi transaction 20.000 dòng). Chạy nền, mỗi lúc một job.
 * Cần bảng {@code location} đã được sửa ({@link AdministrativeUnitService#fixLocations}).
 */
@Service
public class ListingLinkService {

    private static final Logger LOG = LoggerFactory.getLogger(ListingLinkService.class);

    static final int ROWS_PER_STATEMENT = 1000;
    static final int ROWS_PER_TRANSACTION = 20_000;
    private static final int MAX_SAMPLES = 20;

    private final JdbcTemplate jdbcTemplate;
    private final JdbcTemplate streamingJdbcTemplate;
    private final TransactionTemplate transaction;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "listing-link-2025");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong listingsRead = new AtomicLong();
    private final AtomicLong linksWritten = new AtomicLong();
    private volatile Status status = Status.idle();

    public ListingLinkService(DataSource dataSource, PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.streamingJdbcTemplate = new JdbcTemplate(dataSource);
        this.streamingJdbcTemplate.setFetchSize(Integer.MIN_VALUE);
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * Trạng thái job.
     *
     * @param dryRun       true: chỉ đếm, không ghi.
     * @param linksToAdd   số liên kết cần thêm (dry-run) hoặc đã gửi ghi.
     * @param linksWritten số dòng thực sự được thêm (không tính dòng đã có).
     * @param stats        đếm theo nguồn: đơn vị mới lấy từ đâu, đơn vị cũ thêm gì, JSON có khớp xã cũ không.
     * @param samples      ví dụ doanh nghiệp có JSON không khớp xã cũ đang gắn.
     */
    public record Status(
        String state,
        boolean dryRun,
        long listingsRead,
        long linksToAdd,
        long linksWritten,
        Map<String, Long> stats,
        List<String> samples,
        Instant startedAt,
        Instant finishedAt,
        String error
    ) {
        static Status idle() {
            return new Status("IDLE", true, 0, 0, 0, Map.of(), List.of(), null, null, null);
        }
    }

    public Status start(boolean dryRun) {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Đang chạy job gắn doanh nghiệp vào đơn vị hành chính mới");
        }
        listingsRead.set(0);
        linksWritten.set(0);
        status = new Status("RUNNING", dryRun, 0, 0, 0, Map.of(), List.of(), Instant.now(), null, null);
        executor.submit(() -> run(dryRun));
        return status;
    }

    public Status getStatus() {
        Status s = status;
        return "RUNNING".equals(s.state())
            ? new Status(
                  s.state(),
                  s.dryRun(),
                  listingsRead.get(),
                  s.linksToAdd(),
                  linksWritten.get(),
                  s.stats(),
                  s.samples(),
                  s.startedAt(),
                  null,
                  null
              )
            : s;
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    private void run(boolean dryRun) {
        Instant start = Instant.now();
        Map<String, Long> stats = new LinkedHashMap<>();
        List<String> samples = new ArrayList<>();
        long toAdd = 0;
        try {
            Context ctx = loadContext();
            Links links = loadLinks();
            LOG.info("Gắn doanh nghiệp vào ĐVHC mới (dryRun={}): {} liên kết hiện có", dryRun, links.size);

            List<long[]> buffer = new ArrayList<>();
            long[] counter = { 0 };
            streamingJdbcTemplate.query("select id, location_json from listing order by id", rs -> {
                long listingId = rs.getLong(1);
                String[] codes = ListingLinkPlanner.jsonCodes(rs.getString(2));
                Decision decision = ListingLinkPlanner.plan(ctx, codes[0], codes[1], links.of(listingId));
                stats.merge("Đơn vị mới: " + decision.newSource(), 1L, Long::sum);
                stats.merge("Đơn vị cũ: " + decision.oldSource(), 1L, Long::sum);
                if (decision.jsonAgreesWithOldWard() != null) {
                    stats.merge(
                        decision.jsonAgreesWithOldWard() ? "JSON khớp xã cũ đang gắn" : "JSON KHÔNG khớp xã cũ đang gắn",
                        1L,
                        Long::sum
                    );
                    if (!decision.jsonAgreesWithOldWard() && samples.size() < MAX_SAMPLES) {
                        samples.add(
                            "listing " +
                                listingId +
                                ": xã mới theo JSON " +
                                codes[1] +
                                ", xã cũ đang gắn " +
                                Arrays.toString(links.of(listingId))
                        );
                    }
                }
                for (long locationId : decision.add()) {
                    buffer.add(new long[] { listingId, locationId });
                    counter[0]++;
                }
                if (!dryRun && buffer.size() >= ROWS_PER_TRANSACTION) {
                    flush(buffer);
                }
                long read = listingsRead.incrementAndGet();
                if (read % 200_000 == 0) {
                    LOG.info("Gắn ĐVHC mới: đã đọc {} doanh nghiệp, {} liên kết cần thêm", read, counter[0]);
                    status = new Status(
                        "RUNNING",
                        dryRun,
                        read,
                        counter[0],
                        linksWritten.get(),
                        Map.copyOf(stats),
                        List.copyOf(samples),
                        status.startedAt(),
                        null,
                        null
                    );
                }
            });
            if (!dryRun) {
                flush(buffer);
            }
            toAdd = counter[0];
            LOG.info(
                "Gắn ĐVHC mới xong trong {}: {} doanh nghiệp, {} liên kết cần thêm, {} đã ghi. {}",
                Duration.between(start, Instant.now()),
                listingsRead.get(),
                toAdd,
                linksWritten.get(),
                stats
            );
            status = new Status(
                "DONE",
                dryRun,
                listingsRead.get(),
                toAdd,
                linksWritten.get(),
                stats,
                samples,
                status.startedAt(),
                Instant.now(),
                null
            );
        } catch (Exception e) {
            LOG.error("Gắn ĐVHC mới lỗi", e);
            status = new Status(
                "FAILED",
                dryRun,
                listingsRead.get(),
                toAdd,
                linksWritten.get(),
                stats,
                samples,
                status.startedAt(),
                Instant.now(),
                e.getClass().getSimpleName() + ": " + e.getMessage()
            );
        } finally {
            running.set(false);
        }
    }

    /** Ghi buffer: mỗi lệnh INSERT IGNORE nhiều dòng (kết nối không bật rewriteBatchedStatements), một transaction. */
    private void flush(List<long[]> buffer) {
        if (buffer.isEmpty()) {
            return;
        }
        List<long[]> rows = new ArrayList<>(buffer);
        buffer.clear();
        Integer written = transaction.execute(tx -> {
            int total = 0;
            for (int from = 0; from < rows.size(); from += ROWS_PER_STATEMENT) {
                List<long[]> chunk = rows.subList(from, Math.min(from + ROWS_PER_STATEMENT, rows.size()));
                StringBuilder sql = new StringBuilder("insert ignore into rel_listing__location (listing_id, location_id) values ");
                Object[] args = new Object[chunk.size() * 2];
                for (int i = 0; i < chunk.size(); i++) {
                    sql.append(i == 0 ? "(?, ?)" : ", (?, ?)");
                    args[i * 2] = chunk.get(i)[0];
                    args[i * 2 + 1] = chunk.get(i)[1];
                }
                total += jdbcTemplate.update(sql.toString(), args);
            }
            return total;
        });
        linksWritten.addAndGet(written == null ? 0 : written);
    }

    Context loadContext() {
        Context ctx = new Context();
        Map<Long, String> codeById = new HashMap<>();
        List<Object[]> rows = jdbcTemplate.query("select id, slug, type, code, parent_id from location", (rs, n) -> new Object[] {
            rs.getLong(1),
            rs.getString(2),
            rs.getString(3),
            rs.getString(4),
            rs.getObject(5, Long.class),
        });
        rows.forEach(r -> codeById.put((Long) r[0], (String) r[3]));
        for (Object[] r : rows) {
            boolean isNew = AdministrativeUnits.isNew((String) r[1]);
            String provinceCode = r[4] == null ? (String) r[3] : codeById.get((Long) r[4]);
            ctx.addUnit(new Unit((Long) r[0], isNew, (String) r[2], (String) r[3], provinceCode));
        }
        jdbcTemplate.query(
            "select new_province_code, new_ward_code, old_ward_code, old_district_code, old_province_code from location_conversion",
            rs -> {
                ctx.addConversion(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5));
            }
        );
        return ctx;
    }

    /** Liên kết hiện có, sắp theo listing_id (đúng thứ tự khóa chính), tra bằng tìm kiếm nhị phân. */
    static final class Links {

        long[] listingIds = new long[1 << 20];
        long[] locationIds = new long[1 << 20];
        int size;

        void add(long listingId, long locationId) {
            if (size == listingIds.length) {
                listingIds = Arrays.copyOf(listingIds, size * 2);
                locationIds = Arrays.copyOf(locationIds, size * 2);
            }
            listingIds[size] = listingId;
            locationIds[size] = locationId;
            size++;
        }

        long[] of(long listingId) {
            int i = Arrays.binarySearch(listingIds, 0, size, listingId);
            if (i < 0) {
                return new long[0];
            }
            int from = i;
            while (from > 0 && listingIds[from - 1] == listingId) {
                from--;
            }
            int to = i;
            while (to + 1 < size && listingIds[to + 1] == listingId) {
                to++;
            }
            return Arrays.copyOfRange(locationIds, from, to + 1);
        }
    }

    Links loadLinks() {
        Links links = new Links();
        streamingJdbcTemplate.query("select listing_id, location_id from rel_listing__location order by listing_id, location_id", rs -> {
            links.add(rs.getLong(1), rs.getLong(2));
        });
        return links;
    }
}
