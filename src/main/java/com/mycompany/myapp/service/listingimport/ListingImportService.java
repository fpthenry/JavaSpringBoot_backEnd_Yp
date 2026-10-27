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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    public record RowView(ListingImportRow row, Map<String, String> data, Map<String, Object> matched) {}

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

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "listing-import");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<Long> runningBatch = new AtomicReference<>();

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
        PlatformTransactionManager transactionManager
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
            while (source.hasNext()) {
                chunk.add(source.next());
                if (chunk.size() == CHUNK) {
                    processChunk(batchId, sourceCode, dryRun, chunk, state);
                    chunk.clear();
                }
            }
            if (!chunk.isEmpty()) {
                processChunk(batchId, sourceCode, dryRun, chunk, state);
            }
            finish(batchId, ListingImportBatch.DONE, null);
        } catch (Exception e) {
            LOG.error("Nhập doanh nghiệp lỗi (batch {})", batchId, e);
            finish(batchId, ListingImportBatch.FAILED, e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
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
        List<ListingImportRow> results = new ArrayList<>(rows.size());
        List<ListingImportMatcher.Normalized> pending = new ArrayList<>();
        List<ListingImportRow> pendingRows = new ArrayList<>();

        // 1. Kiểm tra trong dòng + trùng trong cùng đợt
        for (ListingImportMatcher.Normalized n : rows) {
            ListingImportRow row = baseRow(batchId, n);
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
            Long id = n.sourceRef() != null ? byRef.get(n.sourceRef()) : null;
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
                    List<String> filled = l == null ? List.of() : writer.fillEmpty(l, n.record(), state.terms, notes);
                    if (filled.isEmpty()) {
                        row.setStatus(Status.UNCHANGED);
                    } else {
                        row.setStatus(Status.MERGED);
                        row.setFilledFields(String.join(",", filled));
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
                    Listing l = writer.create(n, sourceCode, batchId, state.terms, state.slugs, notes);
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

    public Page<ListingImportBatch> batches(Pageable pageable) {
        return batchRepository.findAll(pageable);
    }

    public Optional<ListingImportBatch> batch(long id) {
        return batchRepository.findById(id);
    }

    /** Dòng của đợt (lọc theo trạng thái), kèm dữ liệu gốc và doanh nghiệp đã có bị khớp / nghi trùng. */
    public Page<RowView> rows(long batchId, Status status, Pageable pageable) {
        Page<ListingImportRow> page =
            status == null
                ? rowRepository.findByBatchId(batchId, pageable)
                : rowRepository.findByBatchIdAndStatus(batchId, status, pageable);
        Set<Long> ids = page
            .stream()
            .map(ListingImportRow::getMatchedListingId)
            .filter(id -> id != null)
            .collect(Collectors.toSet());
        Map<Long, Map<String, Object>> matched = new HashMap<>();
        if (!ids.isEmpty()) {
            jdbc.query(
                "select id, wp_id, name, slug, tax_code, phone, mobile, address, status, source from listing where id in (:ids)",
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
                    m.put("address", rs.getString("address"));
                    m.put("status", rs.getString("status"));
                    m.put("source", rs.getString("source"));
                    matched.put(rs.getLong("id"), m);
                }
            );
        }
        return page.map(r -> new RowView(r, data(r), r.getMatchedListingId() == null ? null : matched.get(r.getMatchedListingId())));
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
                    List<String> filled = writer.fillEmpty(l, record, terms, notes);
                    row.setListingId(target);
                    row.setMatchedListingId(target);
                    row.setFilledFields(filled.isEmpty() ? null : String.join(",", filled));
                    row.setStatus(filled.isEmpty() ? Status.UNCHANGED : Status.MERGED);
                    if (filled.isEmpty()) {
                        l = null;
                    } else {
                        l.setUpdatedAt(Instant.now());
                        l.setModifiedAt(Instant.now());
                    }
                }
                case CREATE -> {
                    l = writer.create(n, batch.getSource(), batch.getId(), terms, new HashSet<>(), notes);
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

    private static void deleteQuietly(Path p) {
        try {
            Files.deleteIfExists(p);
        } catch (IOException e) {
            LOG.warn("Không xóa được file tạm {}", p);
        }
    }
}
