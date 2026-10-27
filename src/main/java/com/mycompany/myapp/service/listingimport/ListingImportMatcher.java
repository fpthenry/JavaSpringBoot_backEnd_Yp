package com.mycompany.myapp.service.listingimport;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.mycompany.myapp.domain.Listing;
import com.mycompany.myapp.domain.ListingImportRow.MatchReason;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilterBuilder;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Kiểm tra trùng với doanh nghiệp đã có — theo lô, từ chắc chắn đến gần đúng:
 *
 * <ol>
 *   <li><b>Id nguồn</b> (source, source_ref): nhập lại cùng nguồn → đúng bản ghi đã nhập. Index unique.</li>
 *   <li><b>Mã số thuế</b>: một câu {@code IN (...)} cho cả lô trên index tax_code. MST chi nhánh (-001) là đơn vị khác.</li>
 *   <li><b>Gần đúng</b> (dòng chưa khớp ở 1, 2): Elasticsearch multi-search theo tên (không dấu và có dấu), mỗi dòng lấy 10 ứng viên;
 *       chấm điểm tên + điện thoại + địa chỉ. Ứng viên có MST KHÁC MST của dòng → chắc chắn khác doanh nghiệp, bỏ.</li>
 * </ol>
 * Gồm cả doanh nghiệp nháp (của đợt nhập trước, chưa duyệt) để không tạo trùng giữa các đợt.
 */
@Component
public class ListingImportMatcher {

    /** Ngưỡng nghi trùng — dưới các ngưỡng này coi là doanh nghiệp mới. */
    static final double NAME_ONLY = 0.9;
    static final double NAME_WITH_PHONE = 0.6;
    static final double NAME_WITH_ADDRESS = 0.7;
    static final double ADDRESS_MIN = 0.5;

    /** Ứng viên mỗi dòng: tên phổ biến ("Tân Minh", "Hoàng Long"…) có hàng chục doanh nghiệp, 5 dễ sót doanh nghiệp thật */
    private static final int CANDIDATES = 10;

    private final NamedParameterJdbcTemplate jdbc;
    private final ElasticsearchTemplate elasticsearchTemplate;

    public ListingImportMatcher(DataSource dataSource, ElasticsearchTemplate elasticsearchTemplate) {
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
        this.elasticsearchTemplate = elasticsearchTemplate;
    }

    /** Dòng nhập đã chuẩn hóa. */
    public record Normalized(
        int rowNo,
        ImportRecord record,
        String name,
        String sourceRef,
        String taxCode,
        boolean taxCodeInvalid,
        String phone,
        String mobile,
        Set<String> nameTokens,
        Set<String> addressTokens
    ) {
        public static Normalized of(int rowNo, ImportRecord r) {
            return new Normalized(
                rowNo,
                r,
                ListingImportNormalizer.clean(r.name()),
                ListingImportNormalizer.cut(r.sourceRef(), 255),
                ListingImportNormalizer.taxCode(r.taxCode()),
                ListingImportNormalizer.isInvalidTaxCode(r.taxCode()),
                ListingImportNormalizer.phone(r.phone()),
                ListingImportNormalizer.phone(r.mobile()),
                ListingImportNormalizer.nameTokens(r.name()),
                ListingImportNormalizer.addressTokens(r.address() != null ? r.address() : r.addressNew())
            );
        }

        /** Khóa phát hiện trùng trong cùng đợt khi không có MST: tên + SĐT (hoặc + địa chỉ). */
        String batchKey() {
            String key = String.join(" ", nameTokens);
            if (key.isEmpty()) {
                return null;
            }
            String contact =
                phone != null
                    ? phone.substring(phone.length() - 8)
                    : mobile != null
                      ? mobile.substring(mobile.length() - 8)
                      : String.join(" ", addressTokens);
            return key + "|" + contact;
        }
    }

    /** Doanh nghiệp đã có dùng để so. */
    record Candidate(long id, String name, String taxCode, String phone, String mobile, String address) {}

    /** Kết quả khớp. */
    public record Match(long listingId, MatchReason reason, double score) {}

    // ---------------------------------------------------------------- chắc chắn

    /** (source, source_ref) → id doanh nghiệp đã nhập từ cùng nguồn. */
    public Map<String, Long> bySourceRef(String source, Collection<String> refs) {
        Map<String, Long> result = new HashMap<>();
        if (refs.isEmpty()) {
            return result;
        }
        jdbc.query(
            "select id, source_ref from listing where source = :source and source_ref in (:refs)",
            new MapSqlParameterSource("source", source).addValue("refs", refs),
            rs -> {
                result.put(rs.getString("source_ref"), rs.getLong("id"));
            }
        );
        return result;
    }

    /** MST → id doanh nghiệp (nhiều doanh nghiệp cùng MST — 34 nhóm trong dữ liệu cũ — lấy bản cũ nhất). */
    public Map<String, Long> byTaxCode(Collection<String> taxCodes) {
        Map<String, Long> result = new HashMap<>();
        if (taxCodes.isEmpty()) {
            return result;
        }
        jdbc.query(
            "select tax_code, min(id) id from listing where tax_code in (:codes) group by tax_code",
            new MapSqlParameterSource("codes", taxCodes),
            rs -> {
                result.put(rs.getString("tax_code"), rs.getLong("id"));
            }
        );
        return result;
    }

    // ---------------------------------------------------------------- gần đúng

    /** Nghi trùng cho từng dòng (cùng thứ tự); null = không nghi trùng. Một lượt multi-search cho cả danh sách. */
    public List<Match> suspects(List<Normalized> rows) {
        List<Match> result = new ArrayList<>(rows.size());
        List<NativeQuery> queries = new ArrayList<>();
        List<Integer> queryIndex = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            result.add(null);
            Normalized r = rows.get(i);
            if (r.nameTokens().isEmpty()) {
                continue;
            }
            queries.add(candidateQuery(r));
            queryIndex.add(i);
        }
        if (queries.isEmpty()) {
            return result;
        }
        List<SearchHits<Listing>> hits = elasticsearchTemplate.multiSearch(queries, Listing.class);
        for (int q = 0; q < hits.size(); q++) {
            Normalized r = rows.get(queryIndex.get(q));
            List<Candidate> candidates = hits
                .get(q)
                .getSearchHits()
                .stream()
                .map(SearchHit::getContent)
                .map(l ->
                    new Candidate(
                        l.getId(),
                        l.getName(),
                        l.getTaxCode(),
                        l.getPhone(),
                        l.getMobile(),
                        firstNonBlank(l.getAddress(), l.getAddressNew())
                    )
                )
                .toList();
            result.set(queryIndex.get(q), best(r, candidates));
        }
        return result;
    }

    /**
     * Ứng viên theo tên, đã bỏ "công ty, TNHH…"; 1–2 từ thì phải có đủ, nhiều hơn thì 75%. Tìm trên CẢ HAI field:
     * {@code name.folded} (không dấu — chỉ có sau khi reindex listing) và {@code name} (còn dấu — index nào cũng có).
     * Trước đây chỉ tìm {@code name.folded}: index chưa reindex thì mọi dòng 0 ứng viên → không bao giờ nghi trùng.
     */
    static NativeQuery candidateQuery(Normalized r) {
        String words = String.join(" ", r.nameTokens());
        String accented = ListingImportNormalizer.accentedNameWords(r.record().name());
        Query query = Query.of(q ->
            q.bool(b -> {
                b.should(s -> s.match(mt -> mt.field("name.folded").query(words).minimumShouldMatch("2<75%"))).should(s ->
                    s.matchPhrase(mp -> mp.field("name.folded").query(words).boost(3f))
                );
                if (!accented.isEmpty()) {
                    b.should(s -> s.match(mt -> mt.field("name").query(accented).minimumShouldMatch("2<75%"))).should(s ->
                        s.matchPhrase(mp -> mp.field("name").query(accented).boost(3f))
                    );
                }
                return b.minimumShouldMatch("1");
            })
        );
        return NativeQuery.builder()
            .withQuery(query)
            .withSourceFilter(
                new FetchSourceFilterBuilder().withIncludes("id", "name", "taxCode", "phone", "mobile", "address", "addressNew").build()
            )
            .withMaxResults(CANDIDATES)
            .build();
    }

    /** Ứng viên giống nhất vượt ngưỡng nghi trùng, hoặc null. */
    static Match best(Normalized r, List<Candidate> candidates) {
        Match best = null;
        for (Candidate c : candidates) {
            Match m = score(r, c);
            if (m != null && (best == null || m.score() > best.score())) {
                best = m;
            }
        }
        return best;
    }

    /**
     * Điểm giống nhau của dòng nhập và một doanh nghiệp đã có (0–1), kèm căn cứ; null nếu dưới ngưỡng nghi trùng.
     * Nghi trùng khi: tên ≥ 0,9; hoặc tên ≥ 0,6 và cùng SĐT; hoặc tên ≥ 0,7 và địa chỉ ≥ 0,5.
     */
    static Match score(Normalized r, Candidate c) {
        String candidateTax = ListingImportNormalizer.taxCode(c.taxCode());
        if (r.taxCode() != null && candidateTax != null && !r.taxCode().equals(candidateTax)) {
            return null; // hai MST khác nhau → hai doanh nghiệp khác nhau
        }
        Set<String> candidateTokens = ListingImportNormalizer.nameTokens(c.name());
        double name = ListingImportNormalizer.dice(r.nameTokens(), candidateTokens);
        // Tên viết tắt / bỏ bớt ngành ("An Phước JSC" ⊂ "CP Xây dựng và Thương mại An Phước"): chỉ dùng khi có thêm SĐT / địa chỉ
        double withEvidence = Math.max(name, ListingImportNormalizer.containment(r.nameTokens(), candidateTokens));
        String cp = ListingImportNormalizer.phone(c.phone());
        String cm = ListingImportNormalizer.phone(c.mobile());
        boolean phone =
            ListingImportNormalizer.samePhone(r.phone(), cp) ||
            ListingImportNormalizer.samePhone(r.phone(), cm) ||
            ListingImportNormalizer.samePhone(r.mobile(), cp) ||
            ListingImportNormalizer.samePhone(r.mobile(), cm);
        double address = ListingImportNormalizer.dice(r.addressTokens(), ListingImportNormalizer.addressTokens(c.address()));
        double score = Math.min(1, withEvidence * 0.7 + (phone ? 0.2 : 0) + address * 0.1);
        if (withEvidence >= NAME_WITH_PHONE && phone) {
            return new Match(c.id(), MatchReason.NAME_PHONE, round(score));
        }
        if (withEvidence >= NAME_WITH_ADDRESS && address >= ADDRESS_MIN) {
            return new Match(c.id(), MatchReason.NAME_ADDRESS, round(score));
        }
        if (name >= NAME_ONLY) {
            return new Match(c.id(), MatchReason.NAME, round(score));
        }
        return null;
    }

    private static double round(double v) {
        return Math.round(v * 1000) / 1000.0;
    }

    private static String firstNonBlank(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }

    /** Các MST khác null của danh sách. */
    static Set<String> taxCodes(List<Normalized> rows) {
        return rows
            .stream()
            .map(Normalized::taxCode)
            .filter(t -> t != null)
            .collect(Collectors.toSet());
    }
}
