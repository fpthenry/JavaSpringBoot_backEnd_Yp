package com.mycompany.myapp.service.listingimport;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Bước 2 của đồng bộ API Trang Vàng: đọc bản gốc đã kéo về {@code listing_source_record} (xem {@link DirectusPullService}),
 * không gọi API, nên chạy thử bao nhiêu lần cũng được. Chỉ đọc bản ghi mới hoặc đã đổi so với lần nhập thật gần nhất.
 * Mỗi trang: khớp doanh nghiệp đang có theo api_id → yp_id = wp_id → slug → MST, và chỉ trả về những bản ghi cần xử lý: chưa có
 * doanh nghiệp, hoặc có ô trống mà nguồn có dữ liệu. Nhờ vậy 1,47 triệu bản ghi không sinh 1,47 triệu dòng nhật ký nhập.
 */
public final class DirectusRecordSource implements RecordSource {

    public static final String SOURCE = "api:yp-directus";

    private static final int PAGE_SIZE = 1000;

    /** Trường API → cột listing tương ứng: có giá trị bên API mà ô bên DB trống thì cần bổ sung. */
    static final Map<String, String> FILLABLE = Map.ofEntries(
        Map.entry("taxcode", "tax_code"),
        Map.entry("telephone", "phone"),
        Map.entry("email", "email"),
        Map.entry("website", "website"),
        Map.entry("address", "address"),
        Map.entry("name_en", "name_en"),
        Map.entry("name_alias", "name_alias"),
        Map.entry("representative", "representative"),
        Map.entry("capital", "capital"),
        Map.entry("begin", "founded_year"),
        Map.entry("business_type", "business_type"),
        Map.entry("business_status", "business_status"),
        Map.entry("industry_main", "industry_code"),
        Map.entry("managed", "managed_by"),
        Map.entry("content", "description")
    );

    /** Trạng thái một doanh nghiệp đang có, để biết nguồn có bổ sung được gì không. */
    record ListingState(
        Set<String> blankColumns,
        boolean hasCategory,
        boolean hasLocation,
        boolean hasWard,
        String taxCode,
        Instant changedAt
    ) {}

    private record Stored(long recordId, JsonNode item, Instant updatedAt) {}

    private final boolean dryRun;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate tx;
    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;

    private Map<String, Long> byApiId;
    private long cursor;
    private int rowNo;
    private boolean exhausted;
    private Iterator<SourceRow> current = List.<SourceRow>of().iterator();

    public DirectusRecordSource(boolean dryRun, DataSource dataSource, TransactionTemplate tx, ObjectMapper objectMapper) {
        this.dryRun = dryRun;
        this.tx = tx;
        this.objectMapper = objectMapper;
        this.jdbc = new JdbcTemplate(dataSource);
        this.named = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public boolean hasNext() {
        while (!current.hasNext() && !exhausted) {
            nextPage();
        }
        return current.hasNext();
    }

    @Override
    public SourceRow next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return current.next();
    }

    /** Nhập xong cả đợt: nối bản gốc với doanh nghiệp vừa tạo, đánh dấu đã nhập để lần sau chỉ xử lý bản ghi mới hoặc đã đổi. */
    @Override
    public void onSuccess() {
        if (dryRun) {
            return;
        }
        tx.executeWithoutResult(s -> {
            jdbc.update(
                "update listing_source_record r join listing l on l.source = r.source and l.source_ref = r.source_id " +
                    "set r.listing_id = l.id where r.source = ? and r.listing_id is null",
                SOURCE
            );
            jdbc.update(
                "update listing_source_record set imported_hash = content_hash " +
                    "where source = ? and status = 'published' and (imported_hash is null or imported_hash <> content_hash)",
                SOURCE
            );
        });
    }

    @Override
    public void close() {
        // Không giữ tài nguyên cần đóng
    }

    // ---------------------------------------------------------------- đọc một trang

    private void nextPage() {
        if (byApiId == null) {
            byApiId = loadApiIds();
        }
        List<Stored> page = load();
        if (page.isEmpty()) {
            exhausted = true;
            return;
        }
        cursor = page.get(page.size() - 1).recordId();
        Map<Long, Long> matched = match(page.stream().map(Stored::item).toList());
        Map<Long, ListingState> states = states(matched.values());
        Map<Long, Instant> newest = newestSourceTimes(matched.values());
        if (!dryRun) {
            link(page, matched);
        }
        List<SourceRow> rows = new ArrayList<>();
        for (Stored stored : page) {
            JsonNode item = stored.item();
            Long listingId = matched.get(item.path("id").asLong());
            // Một doanh nghiệp có nhiều bản ghi nguồn: chỉ bản mới nhất được đè thông tin
            boolean refresh =
                listingId != null &&
                (newest.get(listingId) == null || newest.get(listingId).equals(stored.updatedAt())) &&
                refreshes(item, stored.updatedAt(), states.get(listingId));
            if (listingId == null || refresh || hasWork(item, states.get(listingId))) {
                rows.add(
                    new SourceRow(++rowNo, toRecord(item), listingId, ApiRecordSource.text(item.path("slug")), refresh, stored.updatedAt())
                );
            }
        }
        current = rows.iterator();
    }

    private List<Stored> load() {
        return jdbc.query(
            "select id, payload, source_updated_at from listing_source_record where source = ? and id > ? and status = 'published' " +
                "and (imported_hash is null or imported_hash <> content_hash) order by id limit ?",
            (rs, n) -> new Stored(rs.getLong("id"), objectMapper.readTree(rs.getString("payload")), instant(rs, "source_updated_at")),
            SOURCE,
            cursor,
            PAGE_SIZE
        );
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp t = rs.getTimestamp(column);
        return t == null ? null : t.toInstant();
    }

    /** Doanh nghiệp → ngày cập nhật mới nhất trong các bản ghi nguồn của nó. */
    private Map<Long, Instant> newestSourceTimes(Collection<Long> ids) {
        Map<Long, Instant> result = new HashMap<>();
        if (ids.isEmpty()) {
            return result;
        }
        named.query(
            "select listing_id, max(source_updated_at) newest from listing_source_record " +
                "where source = :source and listing_id in (:ids) group by listing_id",
            new MapSqlParameterSource("source", SOURCE).addValue("ids", new HashSet<>(ids)),
            rs -> {
                result.put(rs.getLong("listing_id"), instant(rs, "newest"));
            }
        );
        return result;
    }

    /** Ghi doanh nghiệp đã khớp vào bản gốc. */
    private void link(List<Stored> page, Map<Long, Long> matched) {
        List<Object> args = new ArrayList<>();
        for (Stored stored : page) {
            Long listingId = matched.get(stored.item().path("id").asLong());
            if (listingId != null) {
                args.add(stored.recordId());
                args.add(listingId);
            }
        }
        if (args.isEmpty()) {
            return;
        }
        String rows = String.join(",", Collections.nCopies(args.size() / 2, "row(?, ?)"));
        tx.executeWithoutResult(s ->
            jdbc.update(
                "update listing_source_record r join (values " +
                    rows +
                    ") v (record_id, listing_id) on r.id = v.record_id " +
                    "set r.listing_id = v.listing_id",
                args.toArray()
            )
        );
    }

    // ---------------------------------------------------------------- khớp doanh nghiệp đang có

    private Map<String, Long> loadApiIds() {
        Map<String, Long> ids = new HashMap<>();
        jdbc.query("select id, api_id from listing where api_id is not null and api_id <> ''", rs -> {
            ids.put(rs.getString("api_id"), rs.getLong("id"));
        });
        return ids;
    }

    /** Id nguồn → id doanh nghiệp: api_id, rồi yp_id = wp_id, rồi slug, rồi MST. */
    private Map<Long, Long> match(List<JsonNode> items) {
        Map<Long, Long> result = new HashMap<>();
        Map<Long, Long> byWp = new HashMap<>();
        Map<String, Long> bySlug = new HashMap<>();
        Map<String, Long> byTax = new HashMap<>();
        for (JsonNode item : items) {
            long id = item.path("id").asLong();
            Long listingId = byApiId.get(String.valueOf(id));
            if (listingId != null) {
                result.put(id, listingId);
                continue;
            }
            if (item.path("yp_id").isNumber()) {
                byWp.put(item.path("yp_id").asLong(), id);
            }
            String slug = ApiRecordSource.text(item.path("slug"));
            if (slug != null) {
                bySlug.put(slug, id);
            }
            String tax = ListingImportNormalizer.taxCode(ApiRecordSource.text(item.path("taxcode")));
            if (tax != null) {
                byTax.put(tax, id);
            }
        }
        if (!byWp.isEmpty()) {
            named.query("select id, wp_id from listing where wp_id in (:v)", new MapSqlParameterSource("v", byWp.keySet()), rs -> {
                result.putIfAbsent(byWp.get(rs.getLong("wp_id")), rs.getLong("id"));
            });
        }
        if (!bySlug.isEmpty()) {
            named.query("select id, slug from listing where slug in (:v)", new MapSqlParameterSource("v", bySlug.keySet()), rs -> {
                result.putIfAbsent(bySlug.get(rs.getString("slug")), rs.getLong("id"));
            });
        }
        if (!byTax.isEmpty()) {
            named.query(
                "select tax_code, min(id) id from listing where tax_code in (:v) group by tax_code",
                new MapSqlParameterSource("v", byTax.keySet()),
                rs -> {
                    result.putIfAbsent(byTax.get(rs.getString("tax_code")), rs.getLong("id"));
                }
            );
        }
        return result;
    }

    private Map<Long, ListingState> states(Collection<Long> ids) {
        Map<Long, ListingState> result = new HashMap<>();
        if (ids.isEmpty()) {
            return result;
        }
        StringBuilder sql = new StringBuilder("select l.id");
        List<String> columns = FILLABLE.values().stream().toList();
        for (String c : columns) {
            sql.append(", (l.").append(c).append(" is null or length(trim(l.").append(c).append(")) = 0) ").append(c);
        }
        sql.append(", exists(select 1 from rel_listing__category c where c.listing_id = l.id) has_category")
            .append(", exists(select 1 from rel_listing__location x where x.listing_id = l.id) has_location")
            .append(
                ", exists(select 1 from rel_listing__location x join location u on u.id = x.location_id " +
                    "where x.listing_id = l.id and u.type = 'ward') has_ward"
            )
            .append(", l.tax_code cur_tax, cast(greatest(")
            .append("coalesce(l.modified_at, '1970-01-01'), coalesce(l.updated_at, '1970-01-01')) as datetime(6)) changed_at")
            .append(" from listing l where l.id in (:ids)");
        named.query(sql.toString(), new MapSqlParameterSource("ids", new HashSet<>(ids)), rs -> {
            Set<String> blank = new HashSet<>();
            for (String c : columns) {
                if (rs.getBoolean(c)) {
                    blank.add(c);
                }
            }
            result.put(
                rs.getLong("id"),
                new ListingState(
                    blank,
                    rs.getBoolean("has_category"),
                    rs.getBoolean("has_location"),
                    rs.getBoolean("has_ward"),
                    rs.getString("cur_tax"),
                    instant(rs, "changed_at")
                )
            );
        });
        return result;
    }

    /**
     * Doanh nghiệp đang có: nguồn có ngày cập nhật mới hơn lần đổi gần nhất của doanh nghiệp, và cùng MST.
     * Nguồn không có ngày thì không biết bên nào mới hơn, giữ nguyên.
     */
    static boolean refreshes(JsonNode item, Instant sourceTime, ListingState state) {
        if (state == null || sourceTime == null || state.changedAt() == null || !sourceTime.isAfter(state.changedAt())) {
            return false;
        }
        String tax = ListingImportNormalizer.taxCode(ApiRecordSource.text(item.path("taxcode")));
        String currentTax = ListingImportNormalizer.taxCode(state.taxCode());
        return tax == null || currentTax == null || tax.equals(currentTax);
    }

    /** Doanh nghiệp đang có: nguồn bổ sung được ít nhất một ô trống / ngành / địa phương không. */
    static boolean hasWork(JsonNode item, ListingState state) {
        if (state == null) {
            return false;
        }
        for (Map.Entry<String, String> e : FILLABLE.entrySet()) {
            if (state.blankColumns().contains(e.getValue()) && ApiRecordSource.text(item.path(e.getKey())) != null) {
                return true;
            }
        }
        if (!state.hasCategory() && (categories(item) != null || ApiRecordSource.text(item.path("industry_main")) != null)) {
            return true;
        }
        if (!state.hasLocation() && ApiRecordSource.text(item.path("province")) != null) {
            return true;
        }
        return !state.hasWard() && ApiRecordSource.text(item.path("ward")) != null;
    }

    // ---------------------------------------------------------------- chuyển đổi

    /** Bản ghi nguồn → dòng nhập chuẩn. Logo là id file của Directus, không phải URL ảnh, nên không lấy. */
    static ImportRecord toRecord(JsonNode item) {
        Map<String, String> values = new LinkedHashMap<>();
        put(values, "sourceRef", item, "id");
        put(values, "name", item, "name");
        put(values, "nameEn", item, "name_en");
        put(values, "nameAlias", item, "name_alias");
        put(values, "taxCode", item, "taxcode");
        put(values, "phone", item, "telephone");
        put(values, "email", item, "email");
        put(values, "website", item, "website");
        put(values, "address", item, "address");
        put(values, "province", item, "province");
        put(values, "ward", item, "ward");
        put(values, "representative", item, "representative");
        put(values, "capital", item, "capital");
        put(values, "foundedYear", item, "begin");
        put(values, "businessType", item, "business_type");
        put(values, "businessStatus", item, "business_status");
        put(values, "industryCode", item, "industry_main");
        put(values, "managedBy", item, "managed");
        put(values, "description", item, "content");
        String categories = categories(item);
        if (categories != null) {
            values.put("categories", categories);
        }
        return ImportRecord.of(values);
    }

    private static void put(Map<String, String> values, String field, JsonNode item, String key) {
        String v = ApiRecordSource.text(item.path(key));
        if (v != null) {
            values.put(field, v);
        }
    }

    /** Các mã ngành nối bằng dấu chấm phẩy; null nếu không có. Bỏ mã nội bộ cũ của YP (113YP, 2490YP), không thuộc VSIC. */
    static String categories(JsonNode item) {
        List<String> codes = new ArrayList<>();
        for (JsonNode industry : item.path("industry")) {
            String code = ApiRecordSource.text(industry.path("industries_code"));
            if (code != null && code.chars().allMatch(Character::isDigit)) {
                codes.add(code);
            }
        }
        return codes.isEmpty() ? null : String.join(";", codes);
    }
}
