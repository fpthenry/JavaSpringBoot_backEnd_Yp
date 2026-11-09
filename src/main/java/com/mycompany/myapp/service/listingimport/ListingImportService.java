package com.mycompany.myapp.service.listingimport;

import com.mycompany.myapp.domain.Listing;
import com.mycompany.myapp.domain.ListingImportBatch;
import com.mycompany.myapp.domain.ListingImportRow;
import com.mycompany.myapp.domain.ListingImportRow.MatchReason;
import com.mycompany.myapp.domain.ListingImportRow.Status;
import com.mycompany.myapp.repository.ListingImportBatchRepository;
import com.mycompany.myapp.repository.ListingImportRowRepository;
import com.mycompany.myapp.repository.ListingRepository;
import com.mycompany.myapp.repository.search.ListingSearchFields;
import com.mycompany.myapp.repository.search.ListingSearchRepository;
import com.mycompany.myapp.security.SecurityUtils;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.criteria.Predicate;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Nhập (clone) doanh nghiệp từ nguồn ngoài: file Excel/CSV, API bên thứ ba, JSON gửi thẳng.
 *
 * <p>Mỗi dòng được phân loại ({@link Status}):
 * <ul>
 *   <li>Trùng chắc chắn — cùng id nguồn hoặc cùng MST → <b>bổ sung ô còn trống</b> (MERGED), không có gì thêm → UNCHANGED.</li>
 *   <li>Nghi trùng — tên gần giống + cùng SĐT / địa chỉ → <b>SUSPECT</b>, chờ admin duyệt ({@link #resolve}).</li>
 *   <li>Không trùng → tạo doanh nghiệp <b>nháp</b> (CREATED), duyệt cả đợt bằng {@link #publish}.</li>
 *   <li>Trùng dòng trước trong cùng đợt → DUPLICATE_IN_BATCH; thiếu tên → INVALID.</li>
 * </ul>
 * Chạy nền (một job một lúc), xử lý theo lô {@value #CHUNK} dòng: 1 truy vấn MST + 1 multi-search Elasticsearch mỗi lô.
 * Chạy thử ({@code dryRun}): làm y hệt rồi rollback phần ghi listing — kết quả phân loại đúng như chạy thật.
 */
@Service
public class ListingImportService {

    private static final Logger LOG = LoggerFactory.getLogger(ListingImportService.class);

    static final int CHUNK = 200;
    /** Gửi thẳng JSON: tối đa số bản ghi một lần (nhiều hơn thì dùng file / API). */
    public static final int MAX_JSON_RECORDS = 10_000;

    public enum Action {
        /** Gộp vào doanh nghiệp nghi trùng (hoặc listingId chỉ định): bổ sung ô còn trống. */
        MERGE,
        /** Không phải trùng: tạo doanh nghiệp mới (nháp). */
        CREATE,
        /** Bỏ qua dòng này. */
        SKIP,
    }

    /** Dòng nhập kèm dữ liệu gốc và doanh nghiệp bị nghi trùng — để duyệt cạnh nhau. */
    public record RowView(ListingImportRow row, Map<String, String> data, Map<String, Object> matched, ListingChanges changes) {}

    /** Số dòng đã đổi một trường: điền ô trống, ghi đè. */
    public record FieldStat(String field, long filled, long updated) {}

    /** Một lần doanh nghiệp được nhập. */
    public record HistoryEntry(
        long rowId,
        long batchId,
        String source,
        Instant importedAt,
        String sourceRef,
        Instant sourceUpdatedAt,
        Status status,
        boolean undone,
        ListingChanges changes
    ) {}

    /** Bản nguồn đang nối với doanh nghiệp. */
    public record SourceCopy(String source, String sourceId, Instant sourceUpdatedAt, Instant lastFetchedAt) {}

    public record ListingHistory(List<HistoryEntry> entries, List<SourceCopy> sources) {}

    private final ListingImportBatchRepository batchRepository;
    private final ListingImportRowRepository rowRepository;
    private final ListingRepository listingRepository;
    private final ListingSearchRepository listingSearchRepository;
    private final ListingSearchFields listingSearchFields;
    private final ListingImportMatcher matcher;
    private final ListingImportWriter writer;
    private final DataSource dataSource;
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate tx;
    private final DirectusPullService pullService;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "listing-import");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<Long> runningBatch = new AtomicReference<>();
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);

    public ListingImportService(
        ListingImportBatchRepository batchRepository,
        ListingImportRowRepository rowRepository,
        ListingRepository listingRepository,
        ListingSearchRepository listingSearchRepository,
        ListingSearchFields listingSearchFields,
        ListingImportMatcher matcher,
        ListingImportWriter writer,
        DataSource dataSource,
        ObjectMapper objectMapper,
        PlatformTransactionManager transactionManager,
        DirectusPullService pullService
    ) {
        this.batchRepository = batchRepository;
        this.rowRepository = rowRepository;
        this.listingRepository = listingRepository;
        this.listingSearchRepository = listingSearchRepository;
        this.listingSearchFields = listingSearchFields;
        this.matcher = matcher;
        this.writer = writer;
        this.dataSource = dataSource;
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
        this.objectMapper = objectMapper;
        this.tx = new TransactionTemplate(transactionManager);
        this.pullService = pullService;
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    // ================================================================ bắt đầu một đợt

    /**
     * Nhập từ file .xlsx / .csv. Đọc tiêu đề ngay (sai định dạng → IllegalArgumentException, 400), xử lý ở nền.
     * @throws IllegalStateException đang có đợt nhập khác chạy
     */
    public ListingImportBatch startFile(InputStream content, String fileName, boolean dryRun) throws IOException {
        Path temp = Files.createTempFile("listing-import-", fileName != null && fileName.toLowerCase().endsWith(".csv") ? ".csv" : ".xlsx");
        try {
            Files.copy(content, temp, StandardCopyOption.REPLACE_EXISTING);
            RecordSource source = ImportFileReader.open(temp, fileName);
            return start("file", fileName, dryRun, source, () -> deleteQuietly(temp));
        } catch (RuntimeException | IOException e) {
            deleteQuietly(temp);
            throw e;
        }
    }

    /** Nhập từ API JSON của bên thứ ba (đọc lần lượt từng trang ở nền). */
    public ListingImportBatch startApi(ApiRecordSource.Request request) {
        ApiRecordSource source = new ApiRecordSource(request, objectMapper);
        return start("api:" + request.source(), request.url(), Boolean.TRUE.equals(request.dryRun()), source, () -> {});
    }

    /** Bước nhập của đồng bộ API Trang Vàng: đọc bản gốc đã kéo về {@code listing_source_record}, không gọi API. */
    public ListingImportBatch startDirectus(boolean dryRun) {
        if (pullService.isRunning()) {
            throw new IllegalStateException("Đang kéo dữ liệu từ API, chờ xong rồi nhập");
        }
        DirectusPullService.Summary summary = pullService.summary();
        if (summary.stored() == 0) {
            throw new IllegalArgumentException("Chưa có dữ liệu: bấm Kéo dữ liệu từ API trước");
        }
        if (summary.pending() == 0) {
            throw new IllegalArgumentException("Không còn dữ liệu mới: mọi bản ghi đã được nhập");
        }
        DirectusRecordSource source = new DirectusRecordSource(dryRun, dataSource, tx, objectMapper);
        return start(DirectusRecordSource.SOURCE, "listing_source_record", dryRun, source, () -> {});
    }

    /** Nhập danh sách JSON gửi thẳng (khóa theo tên cột chuẩn hoặc tên cột tiếng Việt như file). */
    public ListingImportBatch startRecords(String sourceCode, List<Map<String, Object>> records, boolean dryRun) {
        if (sourceCode == null || !sourceCode.matches("[a-z0-9-]{1,40}")) {
            throw new IllegalArgumentException("source: 1–40 ký tự chữ thường, số, gạch ngang");
        }
        if (records == null || records.isEmpty() || records.size() > MAX_JSON_RECORDS) {
            throw new IllegalArgumentException("records: 1–" + MAX_JSON_RECORDS + " bản ghi");
        }
        Iterator<Map<String, Object>> it = records.iterator();
        RecordSource source = new RecordSource() {
            private int rowNo;

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public SourceRow next() {
                Map<String, String> values = new HashMap<>();
                it.next().forEach((k, v) -> {
                    String field = ImportRecord.fieldOf(k);
                    String text =
                        v == null
                            ? null
                            : ListingImportNormalizer.clean(
                                  v instanceof List<?> list
                                      ? list.stream().map(String::valueOf).collect(Collectors.joining("; "))
                                      : String.valueOf(v)
                              );
                    if (field != null && text != null) {
                        values.putIfAbsent(field, text);
                    }
                });
                return new SourceRow(++rowNo, ImportRecord.of(values));
            }

            @Override
            public void close() {}
        };
        return start("json:" + sourceCode, records.size() + " bản ghi JSON", dryRun, source, () -> {});
    }

    private ListingImportBatch start(String sourceCode, String sourceName, boolean dryRun, RecordSource source, Runnable cleanup) {
        if (!running.compareAndSet(false, true)) {
            source.close();
            cleanup.run();
            throw new IllegalStateException("Đang có đợt nhập khác chạy (batch " + runningBatch.get() + ")");
        }
        try {
            ListingImportBatch batch = new ListingImportBatch();
            batch.setSource(sourceCode.length() > 50 ? sourceCode.substring(0, 50) : sourceCode);
            batch.setSourceName(sourceName == null ? null : sourceName.substring(0, Math.min(500, sourceName.length())));
            batch.setDryRun(dryRun);
            batch.setStatus(ListingImportBatch.RUNNING);
            batch.setStartedAt(Instant.now());
            batch.setCreatedBy(SecurityUtils.getCurrentUserLogin().orElse(null));
            ListingImportBatch saved = tx.execute(s -> batchRepository.save(batch));
            runningBatch.set(saved.getId());
            executor.submit(() -> run(saved.getId(), saved.getSource(), dryRun, source, cleanup));
            return saved;
        } catch (RuntimeException e) {
            running.set(false);
            source.close();
            cleanup.run();
            throw e;
        }
    }

    /** Đợt đang chạy (nếu có). */
    /**
     * Dừng đợt đang chạy sau lô đang xử lý. Lô đã xong ở đợt nhập thật vẫn được giữ; bản gốc chưa đánh dấu đã nhập nên chạy lại là
     * xử lý tiếp, không ghi trùng. Bước đánh dấu cuối đợt không dừng được.
     */
    public void cancel(long batchId) {
        if (!Long.valueOf(batchId).equals(runningBatch.get())) {
            throw new IllegalStateException("Đợt này không chạy");
        }
        cancelRequested.set(true);
    }

    public Optional<Long> runningBatchId() {
        return running.get() ? Optional.ofNullable(runningBatch.get()) : Optional.empty();
    }

    // ================================================================ xử lý

    /** Trạng thái giữa các lô của một đợt: phát hiện trùng trong cùng đợt, slug đã dùng. */
    private static final class JobState {

        final Map<String, Integer> taxSeen = new HashMap<>();
        final Map<String, Integer> refSeen = new HashMap<>();
        final Map<String, Integer> keySeen = new HashMap<>();
        final Set<String> slugs = new HashSet<>();
        final ImportTermResolver terms;

        JobState(ImportTermResolver terms) {
            this.terms = terms;
        }
    }

    private void run(long batchId, String sourceCode, boolean dryRun, RecordSource source, Runnable cleanup) {
        try (source) {
            JobState state = new JobState(new ImportTermResolver(dataSource));
            List<RecordSource.SourceRow> chunk = new ArrayList<>(CHUNK);
            while (!cancelRequested.get() && source.hasNext()) {
                chunk.add(source.next());
                if (chunk.size() == CHUNK) {
                    processChunk(batchId, sourceCode, dryRun, chunk, state);
                    chunk.clear();
                }
            }
            if (cancelRequested.get()) {
                finish(batchId, ListingImportBatch.CANCELLED, null);
                return;
            }
            if (!chunk.isEmpty()) {
                processChunk(batchId, sourceCode, dryRun, chunk, state);
            }
            source.onSuccess();
            finish(batchId, ListingImportBatch.DONE, null);
        } catch (Exception e) {
            LOG.error("Nhập doanh nghiệp lỗi (batch {})", batchId, e);
            finish(batchId, ListingImportBatch.FAILED, e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            cancelRequested.set(false);
            running.set(false);
            runningBatch.set(null);
            cleanup.run();
        }
    }

    private void processChunk(long batchId, String sourceCode, boolean dryRun, List<RecordSource.SourceRow> chunk, JobState state) {
        List<ListingImportMatcher.Normalized> rows = chunk
            .stream()
            .map(r -> ListingImportMatcher.Normalized.of(r.rowNo(), r.record()))
            .toList();
        Map<Integer, RecordSource.SourceRow> sourceRows = chunk.stream().collect(Collectors.toMap(RecordSource.SourceRow::rowNo, r -> r));
        List<ListingImportRow> results = new ArrayList<>(rows.size());
        List<ListingImportMatcher.Normalized> pending = new ArrayList<>();
        List<ListingImportRow> pendingRows = new ArrayList<>();

        // 1. Kiểm tra trong dòng + trùng trong cùng đợt
        for (ListingImportMatcher.Normalized n : rows) {
            ListingImportRow row = baseRow(batchId, n);
            row.setSourceUpdatedAt(sourceRows.get(n.rowNo()).sourceUpdatedAt());
            results.add(row);
            if (n.taxCodeInvalid()) {
                row.addMessage("MST không hợp lệ: " + n.record().taxCode());
            }
            if (n.name() == null) {
                row.setStatus(Status.INVALID);
                row.addMessage("Thiếu tên doanh nghiệp");
                continue;
            }
            Integer dup = n.taxCode() != null ? state.taxSeen.get(n.taxCode()) : null;
            if (dup == null && n.sourceRef() != null) {
                dup = state.refSeen.get(n.sourceRef());
            }
            String key = n.taxCode() == null ? n.batchKey() : null; // có MST thì MST quyết định, không so tên
            if (dup == null && key != null) {
                dup = state.keySeen.get(key);
            }
            if (dup != null) {
                row.setStatus(Status.DUPLICATE_IN_BATCH);
                row.addMessage("Trùng dòng " + dup + " của đợt này");
                continue;
            }
            if (n.taxCode() != null) {
                state.taxSeen.put(n.taxCode(), n.rowNo());
            }
            if (n.sourceRef() != null) {
                state.refSeen.put(n.sourceRef(), n.rowNo());
            }
            if (key != null) {
                state.keySeen.put(key, n.rowNo());
            }
            pending.add(n);
            pendingRows.add(row);
        }

        // 2. Trùng chắc chắn: id nguồn, MST (mỗi loại 1 truy vấn cho cả lô)
        Map<String, Long> byRef = matcher.bySourceRef(
            sourceCode,
            pending
                .stream()
                .map(ListingImportMatcher.Normalized::sourceRef)
                .filter(r -> r != null)
                .collect(Collectors.toSet())
        );
        Map<String, Long> byTax = matcher.byTaxCode(ListingImportMatcher.taxCodes(pending));
        Map<Integer, ListingImportMatcher.Match> exact = new HashMap<>();
        List<Integer> fuzzyIndex = new ArrayList<>();
        for (int i = 0; i < pending.size(); i++) {
            ListingImportMatcher.Normalized n = pending.get(i);
            Long id = sourceRows.get(n.rowNo()).listingId();
            if (id == null && n.sourceRef() != null) {
                id = byRef.get(n.sourceRef());
            }
            if (id != null) {
                exact.put(i, new ListingImportMatcher.Match(id, MatchReason.SOURCE_REF, 1));
            } else if (n.taxCode() != null && (id = byTax.get(n.taxCode())) != null) {
                exact.put(i, new ListingImportMatcher.Match(id, MatchReason.TAX_CODE, 1));
            } else {
                fuzzyIndex.add(i);
            }
        }

        // 3. Gần đúng: một multi-search Elasticsearch cho các dòng còn lại
        List<ListingImportMatcher.Match> suspects = matcher.suspects(fuzzyIndex.stream().map(pending::get).toList());
        Map<Integer, ListingImportMatcher.Match> suspectByIndex = new HashMap<>();
        for (int k = 0; k < fuzzyIndex.size(); k++) {
            if (suspects.get(k) != null) {
                suspectByIndex.put(fuzzyIndex.get(k), suspects.get(k));
            }
        }

        // 4. Ghi listing (chạy thử: rollback), rồi index Elasticsearch sau khi commit
        List<Listing> toIndex = tx.execute(status -> {
            Map<Long, Listing> existing = loadListings(
                exact.values().stream().map(ListingImportMatcher.Match::listingId).collect(Collectors.toSet())
            );
            List<Listing> changed = new ArrayList<>();
            Instant now = Instant.now();
            for (int i = 0; i < pending.size(); i++) {
                ListingImportMatcher.Normalized n = pending.get(i);
                ListingImportRow row = pendingRows.get(i);
                List<String> notes = new ArrayList<>();
                ListingImportMatcher.Match m = exact.get(i);
                if (m != null) {
                    Listing l = existing.get(m.listingId());
                    row.setMatchedListingId(m.listingId());
                    row.setMatchReason(m.reason());
                    row.setMatchScore(m.score());
                    row.setListingId(dryRun ? null : m.listingId());
                    List<String> filled = new ArrayList<>();
                    ListingChanges.Snapshot before = null;
                    if (l != null) {
                        before = ListingChanges.snapshot(l);
                        if (sourceRows.get(n.rowNo()).refresh()) {
                            filled.addAll(writer.refresh(l, n.record()));
                        }
                        filled.addAll(writer.fillEmpty(l, n.record(), state.terms, notes));
                    }
                    if (filled.isEmpty()) {
                        row.setStatus(Status.UNCHANGED);
                    } else {
                        row.setStatus(Status.MERGED);
                        row.setFilledFields(String.join(",", filled));
                        recordChanges(row, before, l);
                        l.setUpdatedAt(now);
                        l.setModifiedAt(now);
                        changed.add(l);
                    }
                } else if ((m = suspectByIndex.get(i)) != null) {
                    row.setStatus(Status.SUSPECT);
                    row.setMatchedListingId(m.listingId());
                    row.setMatchReason(m.reason());
                    row.setMatchScore(m.score());
                } else {
                    String slug = sourceRows.get(n.rowNo()).slug();
                    Listing l = writer.create(n, sourceCode, batchId, state.terms, state.slugs, notes, slug);
                    if (DirectusRecordSource.SOURCE.equals(sourceCode)) {
                        publishFromApi(l, n.sourceRef(), now);
                    }
                    row.setStatus(Status.CREATED);
                    row.setListingId(dryRun ? null : l.getId());
                    changed.add(l);
                }
                notes.forEach(row::addMessage);
            }
            if (dryRun) {
                status.setRollbackOnly();
                return List.of();
            }
            changed.forEach(listingSearchFields::fill); // id ngành / địa phương cho ES, đọc trong transaction
            return changed;
        });
        if (toIndex != null && !toIndex.isEmpty()) {
            listingSearchRepository.saveAll(toIndex);
        }

        // 5. Lưu kết quả từng dòng + cộng bộ đếm của đợt
        tx.executeWithoutResult(s -> {
            rowRepository.saveAll(results);
            batchRepository.findById(batchId).ifPresent(b -> {
                results.forEach(r -> b.count(r.getStatus()));
                batchRepository.save(b);
            });
        });
    }

    /** Doanh nghiệp mới từ API Trang Vàng lên website ngay, không qua bước nháp. */
    private static void publishFromApi(Listing l, String sourceRef, Instant now) {
        l.setApiId(sourceRef); // như dữ liệu cũ kéo từ API này
        l.setStatus("publish");
        l.setPublishedAt(now);
    }

    private void recordChanges(ListingImportRow row, ListingChanges.Snapshot before, Listing after) {
        ListingChanges changes = before.diff(after);
        row.setChanges(objectMapper.writeValueAsString(changes));
        row.setUpdatedFields(changes.updatedFields().isEmpty() ? null : String.join(",", changes.updatedFields()));
    }

    private ListingImportRow baseRow(long batchId, ListingImportMatcher.Normalized n) {
        ListingImportRow row = new ListingImportRow();
        row.setBatchId(batchId);
        row.setRowNo(n.rowNo());
        row.setSourceRef(n.sourceRef());
        row.setName(n.name());
        row.setTaxCode(n.taxCode());
        row.setPhone(n.phone() != null ? n.phone() : n.mobile());
        row.setData(objectMapper.writeValueAsString(n.record().toMap()));
        return row;
    }

    private Map<Long, Listing> loadListings(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return listingRepository
            .fetchBagRelationships(listingRepository.findAllById(ids))
            .stream()
            .collect(Collectors.toMap(Listing::getId, l -> l));
    }

    private void finish(long batchId, String status, String error) {
        tx.executeWithoutResult(s ->
            batchRepository.findById(batchId).ifPresent(b -> {
                b.setStatus(status);
                b.setError(error);
                b.setFinishedAt(Instant.now());
                batchRepository.save(b);
                LOG.info("Nhập doanh nghiệp xong: {}", b);
            })
        );
    }

    // ================================================================ duyệt

    /** @param dryRun false: chỉ các lần nhập thật, true: chỉ các lần kiểm tra, null: tất cả */
    public Page<ListingImportBatch> batches(Boolean dryRun, Pageable pageable) {
        return dryRun == null ? batchRepository.findAll(pageable) : batchRepository.findByDryRun(dryRun, pageable);
    }

    public Optional<ListingImportBatch> batch(long id) {
        return batchRepository.findById(id);
    }

    /**
     * Dòng của đợt (lọc theo trạng thái), kèm dữ liệu gốc và doanh nghiệp đã có bị khớp / nghi trùng — đủ các trường để
     * màn hình duyệt so cạnh nhau (tên, MST, SĐT, email, website, địa chỉ cũ/mới, người đại diện).
     */
    public Page<RowView> rows(long batchId, Status status, String q, String field, Pageable pageable) {
        Page<ListingImportRow> page = rowRepository.findAll(rowFilter(batchId, status, q, field), pageable);
        Set<Long> ids = page
            .stream()
            .map(ListingImportRow::getMatchedListingId)
            .filter(id -> id != null)
            .collect(Collectors.toSet());
        Map<Long, Map<String, Object>> matched = new HashMap<>();
        if (!ids.isEmpty()) {
            jdbc.query(
                "select id, wp_id, name, slug, tax_code, phone, mobile, email, website, address, address_new, representative, status, source " +
                    "from listing where id in (:ids)",
                new MapSqlParameterSource("ids", ids),
                rs -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong("id"));
                    m.put("wpId", rs.getObject("wp_id"));
                    m.put("name", rs.getString("name"));
                    m.put("slug", rs.getString("slug"));
                    m.put("taxCode", rs.getString("tax_code"));
                    m.put("phone", rs.getString("phone"));
                    m.put("mobile", rs.getString("mobile"));
                    m.put("email", rs.getString("email"));
                    m.put("website", rs.getString("website"));
                    m.put("address", rs.getString("address"));
                    m.put("addressNew", rs.getString("address_new"));
                    m.put("representative", rs.getString("representative"));
                    m.put("status", rs.getString("status"));
                    m.put("source", rs.getString("source"));
                    matched.put(rs.getLong("id"), m);
                }
            );
        }
        return page.map(r ->
            new RowView(r, data(r), r.getMatchedListingId() == null ? null : matched.get(r.getMatchedListingId()), changes(r.getChanges()))
        );
    }

    /** Lọc dòng của đợt: trạng thái, trường đã đổi, tìm theo tên (không phân biệt dấu), MST, id nguồn. */
    private static Specification<ListingImportRow> rowFilter(long batchId, Status status, String q, String field) {
        return (root, query, cb) -> {
            List<Predicate> all = new ArrayList<>();
            all.add(cb.equal(root.get("batchId"), batchId));
            if (status != null) {
                all.add(cb.equal(root.get("status"), status));
            }
            if (field != null && !field.isBlank()) {
                all.add(cb.like(cb.concat(cb.concat(",", root.<String>get("filledFields")), ","), "%," + field + ",%"));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim() + "%";
                all.add(
                    cb.or(cb.like(root.get("name"), like), cb.like(root.get("taxCode"), like), cb.equal(root.get("sourceRef"), q.trim()))
                );
            }
            return cb.and(all.toArray(Predicate[]::new));
        };
    }

    private ListingChanges changes(String json) {
        return json == null ? null : objectMapper.readValue(json, ListingChanges.class);
    }

    /** Số dòng đã đổi từng trường của đợt, nhiều nhất trước. Bộ lọc nhanh trên trang kết quả. */
    public List<FieldStat> fieldStats(long batchId) {
        Map<String, long[]> counts = new HashMap<>();
        jdbc.query(
            "select filled_fields, updated_fields, count(*) c from listing_import_row " +
                "where batch_id = :batch and filled_fields is not null group by filled_fields, updated_fields",
            new MapSqlParameterSource("batch", batchId),
            rs -> {
                Set<String> updated = Set.of(
                    rs.getString("updated_fields") == null ? new String[0] : rs.getString("updated_fields").split(",")
                );
                for (String f : rs.getString("filled_fields").split(",")) {
                    counts.computeIfAbsent(f, k -> new long[2])[updated.contains(f) ? 1 : 0] += rs.getLong("c");
                }
            }
        );
        return counts
            .entrySet()
            .stream()
            .map(e -> new FieldStat(e.getKey(), e.getValue()[0], e.getValue()[1]))
            .sorted(Comparator.comparingLong((FieldStat s) -> s.filled() + s.updated()).reversed())
            .toList();
    }

    /** Xuất các dòng đang lọc ra CSV (Excel mở được), mỗi trường đã đổi một dòng. */
    public void exportRows(long batchId, Status status, String q, String field, Writer out) throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("batch", batchId);
        StringBuilder sql = new StringBuilder(
            "select row_no, status, name, tax_code, source_ref, source_updated_at, coalesce(listing_id, matched_listing_id) listing, changes, filled_fields " +
                "from listing_import_row where batch_id = :batch"
        );
        if (status != null) {
            sql.append(" and status = :status");
            params.put("status", status.name());
        }
        if (field != null && !field.isBlank()) {
            sql.append(" and concat(',', filled_fields, ',') like :field");
            params.put("field", "%," + field + ",%");
        }
        if (q != null && !q.isBlank()) {
            sql.append(" and (name like :like or tax_code like :like or source_ref = :q)");
            params.put("like", "%" + q.trim() + "%");
            params.put("q", q.trim());
        }
        sql.append(" order by row_no");
        out.write('\uFEFF'); // Excel cần BOM để đọc đúng tiếng Việt
        CSVPrinter csv = new CSVPrinter(out, CSVFormat.DEFAULT);
        csv.printRecord(
            "Dòng",
            "Kết quả",
            "Tên",
            "MST",
            "ID nguồn",
            "Ngày cập nhật nguồn",
            "ID doanh nghiệp",
            "Trường",
            "Loại",
            "Giá trị cũ",
            "Giá trị mới"
        );
        jdbc.query(sql.toString(), params, rs -> {
            try {
                List<Object> head = new ArrayList<>(
                    Arrays.asList(
                        rs.getInt("row_no"),
                        rs.getString("status"),
                        rs.getString("name"),
                        rs.getString("tax_code"),
                        rs.getString("source_ref"),
                        rs.getTimestamp("source_updated_at"),
                        rs.getObject("listing")
                    )
                );
                ListingChanges c = changes(rs.getString("changes"));
                List<ListingChanges.FieldChange> fields =
                    c == null
                        ? List.of()
                        : c
                              .fields()
                              .stream()
                              .filter(f -> !ListingChanges.LOCATION_JSON.equals(f.field()))
                              .toList();
                if (fields.isEmpty()) {
                    head.add(rs.getString("filled_fields"));
                    csv.printRecord(head);
                }
                for (ListingChanges.FieldChange f : fields) {
                    List<Object> line = new ArrayList<>(head);
                    line.addAll(
                        Arrays.asList(
                            f.field(),
                            f.updated() ? "Cập nhật" : "Điền mới",
                            ListingImportNormalizer.cut(f.oldValue(), 300),
                            ListingImportNormalizer.cut(f.newValue(), 300)
                        )
                    );
                    csv.printRecord(line);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        csv.flush();
    }

    /** Lịch sử nhập của một doanh nghiệp, mới nhất trước, và các bản nguồn đang nối với nó. */
    public ListingHistory history(long listingId) {
        MapSqlParameterSource params = new MapSqlParameterSource("listing", listingId);
        List<HistoryEntry> entries = jdbc.query(
            "select r.id, r.batch_id, b.source, b.started_at, b.status batch_status, r.source_ref, r.source_updated_at, r.status, r.changes " +
                "from listing_import_row r join listing_import_batch b on b.id = r.batch_id " +
                "where r.listing_id = :listing and b.dry_run = 0 and r.status in ('CREATED', 'MERGED') order by r.id desc limit 50",
            params,
            (rs, n) ->
                new HistoryEntry(
                    rs.getLong("id"),
                    rs.getLong("batch_id"),
                    rs.getString("source"),
                    instant(rs.getTimestamp("started_at")),
                    rs.getString("source_ref"),
                    instant(rs.getTimestamp("source_updated_at")),
                    Status.valueOf(rs.getString("status")),
                    ListingImportBatch.UNDONE.equals(rs.getString("batch_status")),
                    changes(rs.getString("changes"))
                )
        );
        List<SourceCopy> sources = jdbc.query(
            "select source, source_id, source_updated_at, last_fetched_at from listing_source_record " +
                "where listing_id = :listing order by source_updated_at desc, id desc limit 20",
            params,
            (rs, n) ->
                new SourceCopy(
                    rs.getString("source"),
                    rs.getString("source_id"),
                    instant(rs.getTimestamp("source_updated_at")),
                    instant(rs.getTimestamp("last_fetched_at"))
                )
        );
        return new ListingHistory(entries, sources);
    }

    private static Instant instant(Timestamp t) {
        return t == null ? null : t.toInstant();
    }

    /**
     * Hoàn tác đợt nhập thật (chạy nền): trả doanh nghiệp đã cập nhật về giá trị cũ, trường đã đổi tiếp sau đợt này thì giữ nguyên.
     * Doanh nghiệp đợt này tạo chuyển về nháp để gỡ khỏi website, không xóa. Bản gốc đánh dấu chưa nhập, nhập lại được.
     */
    public ListingImportBatch undo(long batchId) {
        ListingImportBatch batch = batchRepository
            .findById(batchId)
            .orElseThrow(() -> new IllegalArgumentException("Không có đợt " + batchId));
        if (batch.isDryRun() || !ListingImportBatch.DONE.equals(batch.getStatus())) {
            throw new IllegalStateException("Chỉ hoàn tác được đợt nhập thật đã chạy xong");
        }
        Integer missing = jdbc.queryForObject(
            "select count(*) from listing_import_row where batch_id = :batch and status = 'MERGED' and changes is null",
            new MapSqlParameterSource("batch", batchId),
            Integer.class
        );
        if (missing != null && missing > 0) {
            throw new IllegalStateException("Đợt này chưa lưu giá trị cũ nên không hoàn tác được");
        }
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Đang có đợt nhập khác chạy (batch " + runningBatch.get() + ")");
        }
        try {
            batch.setStatus(ListingImportBatch.UNDOING);
            ListingImportBatch saved = tx.execute(s -> batchRepository.save(batch));
            runningBatch.set(batchId);
            String user = SecurityUtils.getCurrentUserLogin().orElse(null);
            executor.submit(() -> runUndo(batchId, saved.getSource(), user));
            return saved;
        } catch (RuntimeException e) {
            running.set(false);
            throw e;
        }
    }

    private void runUndo(long batchId, String source, String user) {
        try {
            int skipped = 0;
            long cursor = Long.MAX_VALUE;
            while (true) {
                List<ListingImportRow> rows = rowRepository.findByBatchIdAndStatusInAndIdLessThanOrderByIdDesc(
                    batchId,
                    List.of(Status.MERGED, Status.CREATED),
                    cursor,
                    PageRequest.of(0, CHUNK)
                );
                if (rows.isEmpty()) {
                    break;
                }
                cursor = rows.get(rows.size() - 1).getId();
                int[] kept = new int[1];
                List<Listing> changed = tx.execute(s -> undoChunk(rows, kept));
                if (changed != null && !changed.isEmpty()) {
                    listingSearchRepository.saveAll(changed);
                }
                skipped += kept[0];
            }
            int totalSkipped = skipped;
            tx.executeWithoutResult(s -> {
                if (DirectusRecordSource.SOURCE.equals(source)) {
                    jdbc.update(
                        "update listing_source_record r join listing_import_row i on i.source_ref = r.source_id " +
                            "set r.imported_hash = null " +
                            "where r.source = :source and i.batch_id = :batch and i.status in ('CREATED', 'MERGED')",
                        new MapSqlParameterSource("source", source).addValue("batch", batchId)
                    );
                }
                batchRepository.findById(batchId).ifPresent(b -> {
                    b.setStatus(ListingImportBatch.UNDONE);
                    b.setUndoneAt(Instant.now());
                    b.setUndoneBy(user);
                    b.setUndoSkipped(totalSkipped);
                    batchRepository.save(b);
                });
            });
            LOG.info("Hoàn tác đợt nhập {}: giữ nguyên {} trường", batchId, skipped);
        } catch (Exception e) {
            LOG.error("Hoàn tác đợt nhập lỗi (batch {})", batchId, e);
            tx.executeWithoutResult(s ->
                batchRepository.findById(batchId).ifPresent(b -> {
                    b.setStatus(ListingImportBatch.DONE);
                    b.setError("Hoàn tác lỗi: " + e.getMessage());
                    batchRepository.save(b);
                })
            );
        } finally {
            running.set(false);
            runningBatch.set(null);
        }
    }

    private List<Listing> undoChunk(List<ListingImportRow> rows, int[] kept) {
        Map<Long, Listing> listings = loadListings(
            rows
                .stream()
                .map(ListingImportRow::getListingId)
                .filter(id -> id != null)
                .collect(Collectors.toSet())
        );
        Set<Listing> changed = new LinkedHashSet<>();
        for (ListingImportRow row : rows) {
            Listing l = listings.get(row.getListingId());
            if (l == null) {
                continue;
            }
            if (row.getStatus() == Status.CREATED) {
                l.setStatus(ListingImportWriter.DRAFT);
                l.setPublishedAt(null);
                l.setUpdatedAt(Instant.now());
            } else {
                kept[0] += changes(row.getChanges()).undo(l);
            }
            changed.add(l);
        }
        changed.forEach(listingSearchFields::fill);
        return new ArrayList<>(changed);
    }

    private Map<String, String> data(ListingImportRow r) {
        return r.getData() == null ? Map.of() : objectMapper.readValue(r.getData(), new TypeReference<Map<String, String>>() {});
    }

    /**
     * Duyệt một dòng nghi trùng.
     * @param listingId gộp vào doanh nghiệp khác với doanh nghiệp hệ thống nghi trùng (tùy chọn, chỉ với MERGE)
     */
    public ListingImportRow resolve(long rowId, Action action, Long listingId) {
        ListingImportRow row = rowRepository.findById(rowId).orElseThrow(() -> new IllegalArgumentException("Không có dòng " + rowId));
        ListingImportBatch batch = batchRepository.findById(row.getBatchId()).orElseThrow();
        if (row.getStatus() != Status.SUSPECT) {
            throw new IllegalStateException("Chỉ duyệt được dòng nghi trùng (dòng này: " + row.getStatus() + ")");
        }
        if (batch.isDryRun()) {
            throw new IllegalStateException("Đợt chạy thử không ghi dữ liệu — nhập lại không chọn chạy thử");
        }
        if (ListingImportBatch.RUNNING.equals(batch.getStatus())) {
            throw new IllegalStateException("Đợt nhập đang chạy, chờ xong rồi duyệt");
        }
        ImportTermResolver terms = new ImportTermResolver(dataSource);
        String user = SecurityUtils.getCurrentUserLogin().orElse(null);
        Listing changed = tx.execute(s -> {
            ImportRecord record = ImportRecord.of(data(row));
            ListingImportMatcher.Normalized n = ListingImportMatcher.Normalized.of(row.getRowNo() == null ? 0 : row.getRowNo(), record);
            List<String> notes = new ArrayList<>();
            Listing l = null;
            switch (action) {
                case MERGE -> {
                    long target = listingId != null ? listingId : row.getMatchedListingId();
                    l = listingRepository
                        .findOneWithEagerRelationships(target)
                        .orElseThrow(() -> new IllegalArgumentException("Không có doanh nghiệp " + target));
                    ListingChanges.Snapshot before = ListingChanges.snapshot(l);
                    List<String> filled = writer.fillEmpty(l, record, terms, notes);
                    row.setListingId(target);
                    row.setMatchedListingId(target);
                    row.setFilledFields(filled.isEmpty() ? null : String.join(",", filled));
                    row.setStatus(filled.isEmpty() ? Status.UNCHANGED : Status.MERGED);
                    if (filled.isEmpty()) {
                        l = null;
                    } else {
                        recordChanges(row, before, l);
                        l.setUpdatedAt(Instant.now());
                        l.setModifiedAt(Instant.now());
                    }
                }
                case CREATE -> {
                    l = writer.create(n, batch.getSource(), batch.getId(), terms, new HashSet<>(), notes);
                    if (DirectusRecordSource.SOURCE.equals(batch.getSource())) {
                        publishFromApi(l, n.sourceRef(), Instant.now());
                    }
                    row.setListingId(l.getId());
                    row.setStatus(Status.CREATED);
                }
                case SKIP -> row.setStatus(Status.SKIPPED);
            }
            notes.forEach(row::addMessage);
            row.setResolvedBy(user);
            row.setResolvedAt(Instant.now());
            rowRepository.save(row);
            batch.resolveSuspect(row.getStatus());
            batchRepository.save(batch);
            if (l != null) {
                listingSearchFields.fill(l);
            }
            return l;
        });
        if (changed != null) {
            listingSearchRepository.save(changed);
        }
        return row;
    }

    /** Xuất bản các doanh nghiệp nháp do đợt này tạo (hiện lên website, sitemap). @return số doanh nghiệp đã xuất bản */
    public int publish(long batchId) {
        ListingImportBatch batch = batchRepository
            .findById(batchId)
            .orElseThrow(() -> new IllegalArgumentException("Không có đợt " + batchId));
        if (ListingImportBatch.RUNNING.equals(batch.getStatus())) {
            throw new IllegalStateException("Đợt nhập đang chạy, chờ xong rồi xuất bản");
        }
        List<Long> ids = jdbc.queryForList(
            "select id from listing where import_batch_id = :batch and status = :draft",
            new MapSqlParameterSource("batch", batchId).addValue("draft", ListingImportWriter.DRAFT),
            Long.class
        );
        for (int from = 0; from < ids.size(); from += 500) {
            List<Long> part = ids.subList(from, Math.min(ids.size(), from + 500));
            List<Listing> listings = tx.execute(s -> {
                Instant now = Instant.now();
                List<Listing> ls = listingRepository.fetchBagRelationships(listingRepository.findAllById(part));
                for (Listing l : ls) {
                    l.setStatus("publish");
                    l.setPublishedAt(now);
                    l.setUpdatedAt(now);
                    listingSearchFields.fill(l);
                }
                return ls;
            });
            listingSearchRepository.saveAll(listings);
        }
        LOG.info("Xuất bản {} doanh nghiệp của đợt nhập {}", ids.size(), batchId);
        return ids.size();
    }

    /** Số doanh nghiệp nháp do đợt này tạo mà chưa xuất bản. */
    public long draftCount(long batchId) {
        return jdbc.queryForObject(
            "select count(*) from listing where import_batch_id = :batch and status = :draft",
            new MapSqlParameterSource("batch", batchId).addValue("draft", ListingImportWriter.DRAFT),
            Long.class
        );
    }

    private static void deleteQuietly(Path p) {
        try {
            Files.deleteIfExists(p);
        } catch (IOException e) {
            LOG.warn("Không xóa được file tạm {}", p);
        }
    }
}
