package com.mycompany.myapp.service.dvhc;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
 * Điền {@code listing.address_new}: địa chỉ theo địa giới mới, theo {@link NewAddressPlanner}. Xã mới, tỉnh mới lấy từ
 * liên kết đơn vị mới của doanh nghiệp (bước "link-listings"). Chỉ ghi dòng có giá trị thay đổi; chạy lại được.
 * <p>
 * <b>Ghi đè</b> address_new đã có (kể cả sửa tay) nếu khác giá trị tính được; dùng {@code overwrite=false} để chỉ điền
 * dòng đang trống. Sau khi ghi cần reindex Elasticsearch cho listing để tìm được theo địa chỉ mới.
 */
@Service
public class ListingAddressService {

    private static final Logger LOG = LoggerFactory.getLogger(ListingAddressService.class);

    static final int ROWS_PER_STATEMENT = 1000;
    static final int ROWS_PER_TRANSACTION = 20_000;
    private static final int MAX_SAMPLES = 40;
    /** Tên gọi tắt của tỉnh hay gặp trong địa chỉ. */
    private static final Set<String> PROVINCE_ALIASES = Set.of("hcm", "tphcm", "sai gon", "hn");

    private final JdbcTemplate jdbcTemplate;
    private final JdbcTemplate streamingJdbcTemplate;
    private final TransactionTemplate transaction;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "listing-address-2025");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong listingsRead = new AtomicLong();
    private final AtomicLong rowsWritten = new AtomicLong();
    private volatile Status status = Status.idle();

    public ListingAddressService(DataSource dataSource, PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.streamingJdbcTemplate = new JdbcTemplate(dataSource);
        this.streamingJdbcTemplate.setFetchSize(Integer.MIN_VALUE);
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * @param changed số dòng address_new cần đổi (dry-run) hoặc đã gửi ghi.
     * @param stats   đếm theo kết quả (tạo mới, đã là địa chỉ mới, chưa có xã mới, địa chỉ rỗng).
     * @param samples ví dụ "địa chỉ cũ → địa chỉ mới".
     */
    public record Status(
        String state,
        boolean dryRun,
        boolean overwrite,
        long listingsRead,
        long changed,
        long rowsWritten,
        Map<String, Long> stats,
        List<String> samples,
        Instant startedAt,
        Instant finishedAt,
        String error
    ) {
        static Status idle() {
            return new Status("IDLE", true, true, 0, 0, 0, Map.of(), List.of(), null, null, null);
        }
    }

    public Status start(boolean dryRun, boolean overwrite) {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Đang chạy job tạo địa chỉ mới");
        }
        listingsRead.set(0);
        rowsWritten.set(0);
        status = new Status("RUNNING", dryRun, overwrite, 0, 0, 0, Map.of(), List.of(), Instant.now(), null, null);
        executor.submit(() -> run(dryRun, overwrite));
        return status;
    }

    public Status getStatus() {
        Status s = status;
        return "RUNNING".equals(s.state())
            ? new Status(
                  s.state(),
                  s.dryRun(),
                  s.overwrite(),
                  listingsRead.get(),
                  s.changed(),
                  rowsWritten.get(),
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

    /** Một đơn vị, kèm mã tỉnh gốc (đi ngược lên tới cấp tỉnh). */
    record UnitInfo(long id, boolean isNew, String type, String name, String rootProvinceCode) {}

    private void run(boolean dryRun, boolean overwrite) {
        Instant start = Instant.now();
        Map<String, Long> stats = new LinkedHashMap<>();
        List<String> samples = new ArrayList<>();
        long[] changed = { 0 };
        try {
            Map<Long, UnitInfo> units = loadUnits();
            Set<String> provinceNames = new HashSet<>(PROVINCE_ALIASES);
            Map<String, String> newProvinceNames = new HashMap<>();
            Map<String, Set<String>> districtNamesByOldProvince = new HashMap<>();
            for (UnitInfo u : units.values()) {
                if (AdministrativeUnits.PROVINCE.equals(u.type())) {
                    provinceNames.add(NewAddressPlanner.nameKey(u.name()));
                    if (u.isNew()) {
                        newProvinceNames.put(u.rootProvinceCode(), u.name());
                    }
                } else if (!u.isNew() && AdministrativeUnits.DISTRICT.equals(u.type()) && u.rootProvinceCode() != null) {
                    districtNamesByOldProvince
                        .computeIfAbsent(u.rootProvinceCode(), k -> new HashSet<>())
                        .add(NewAddressPlanner.nameKey(u.name()));
                }
            }
            Map<String, Set<String>> oldProvincesByNewProvince = new HashMap<>();
            jdbcTemplate.query("select distinct new_province_code, old_province_code from location_conversion", rs -> {
                oldProvincesByNewProvince.computeIfAbsent(rs.getString(1), k -> new HashSet<>()).add(rs.getString(2));
            });
            ListingLinkService.Links links = loadLinks();
            List<Object[]> buffer = new ArrayList<>();

            streamingJdbcTemplate.query("select id, address, address_new from listing order by id", rs -> {
                long listingId = rs.getLong(1);
                String address = rs.getString(2);
                String current = rs.getString(3);
                UnitInfo newWard = null;
                UnitInfo newProvince = null;
                Set<String> oldProvinceCodes = new HashSet<>();
                Set<String> localWardNames = new HashSet<>();
                for (long locationId : links.of(listingId)) {
                    UnitInfo u = units.get(locationId);
                    if (u == null) {
                        continue;
                    }
                    if (u.isNew()) {
                        if (AdministrativeUnits.WARD.equals(u.type())) {
                            newWard = u;
                        } else {
                            newProvince = u;
                        }
                    } else {
                        if (u.rootProvinceCode() != null) {
                            oldProvinceCodes.add(u.rootProvinceCode());
                        }
                        if (AdministrativeUnits.WARD.equals(u.type())) {
                            localWardNames.add(NewAddressPlanner.nameKey(u.name()));
                        }
                    }
                }
                String provinceCode =
                    newWard != null ? newWard.rootProvinceCode() : newProvince == null ? null : newProvince.rootProvinceCode();
                if (provinceCode != null) {
                    oldProvinceCodes.addAll(oldProvincesByNewProvince.getOrDefault(provinceCode, Set.of()));
                }
                Set<String> districtNames = new HashSet<>();
                oldProvinceCodes.forEach(code -> districtNames.addAll(districtNamesByOldProvince.getOrDefault(code, Set.of())));

                NewAddressPlanner.Result result = NewAddressPlanner.plan(
                    address,
                    newWard == null ? null : newWard.name(),
                    provinceCode == null ? null : newProvinceNames.get(provinceCode),
                    provinceNames,
                    districtNames,
                    localWardNames
                );
                stats.merge(result.outcome(), 1L, Long::sum);
                String target = result.addressNew();
                boolean write = !Objects.equals(target, current) && (overwrite || current == null || current.isBlank());
                if (write) {
                    changed[0]++;
                    buffer.add(new Object[] { listingId, target });
                    if (samples.size() < MAX_SAMPLES && target != null && listingId % 7 == 0) {
                        samples.add(address + "  →  " + target);
                    }
                }
                if (!dryRun && buffer.size() >= ROWS_PER_TRANSACTION) {
                    flush(buffer);
                }
                long read = listingsRead.incrementAndGet();
                if (read % 200_000 == 0) {
                    status = new Status(
                        "RUNNING",
                        dryRun,
                        overwrite,
                        read,
                        changed[0],
                        rowsWritten.get(),
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
            LOG.info(
                "Tạo địa chỉ mới xong trong {}: {} doanh nghiệp, {} cần đổi, {} đã ghi. {}",
                Duration.between(start, Instant.now()),
                listingsRead.get(),
                changed[0],
                rowsWritten.get(),
                stats
            );
            status = new Status(
                "DONE",
                dryRun,
                overwrite,
                listingsRead.get(),
                changed[0],
                rowsWritten.get(),
                stats,
                samples,
                status.startedAt(),
                Instant.now(),
                null
            );
        } catch (Exception e) {
            LOG.error("Tạo địa chỉ mới lỗi", e);
            status = new Status(
                "FAILED",
                dryRun,
                overwrite,
                listingsRead.get(),
                changed[0],
                rowsWritten.get(),
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

    /** UPDATE ... JOIN (VALUES ROW(...)): mỗi lệnh 1.000 dòng (kết nối không bật rewriteBatchedStatements). */
    private void flush(List<Object[]> buffer) {
        if (buffer.isEmpty()) {
            return;
        }
        List<Object[]> rows = new ArrayList<>(buffer);
        buffer.clear();
        transaction.executeWithoutResult(tx -> {
            for (int from = 0; from < rows.size(); from += ROWS_PER_STATEMENT) {
                List<Object[]> chunk = rows.subList(from, Math.min(from + ROWS_PER_STATEMENT, rows.size()));
                StringBuilder sql = new StringBuilder("update listing l join (values ");
                Object[] args = new Object[chunk.size() * 2];
                for (int i = 0; i < chunk.size(); i++) {
                    sql.append(i == 0 ? "row(?, ?)" : ", row(?, ?)");
                    args[i * 2] = chunk.get(i)[0];
                    args[i * 2 + 1] = chunk.get(i)[1];
                }
                sql.append(") as v (id, address_new) on l.id = v.id set l.address_new = v.address_new");
                jdbcTemplate.update(sql.toString(), args);
                rowsWritten.addAndGet(chunk.size());
            }
        });
    }

    Map<Long, UnitInfo> loadUnits() {
        Map<Long, Object[]> rows = new HashMap<>();
        jdbcTemplate.query("select id, slug, type, name, code, parent_id from location", rs -> {
            rows.put(rs.getLong(1), new Object[] {
                rs.getString(2),
                rs.getString(3),
                rs.getString(4),
                rs.getString(5),
                rs.getObject(6, Long.class),
            });
        });
        Map<Long, UnitInfo> units = new HashMap<>();
        for (Map.Entry<Long, Object[]> e : rows.entrySet()) {
            // Đi ngược lên tỉnh để lấy mã tỉnh gốc
            Object[] row = e.getValue();
            Object[] root = row;
            Set<Long> seen = new HashSet<>();
            Long parent = (Long) row[4];
            while (parent != null && seen.add(parent) && rows.containsKey(parent)) {
                root = rows.get(parent);
                parent = (Long) root[4];
            }
            units.put(
                e.getKey(),
                new UnitInfo(e.getKey(), AdministrativeUnits.isNew((String) row[0]), (String) row[1], (String) row[2], (String) root[3])
            );
        }
        return units;
    }

    ListingLinkService.Links loadLinks() {
        ListingLinkService.Links links = new ListingLinkService.Links();
        streamingJdbcTemplate.query("select listing_id, location_id from rel_listing__location order by listing_id, location_id", rs -> {
            links.add(rs.getLong(1), rs.getLong(2));
        });
        return links;
    }
}
