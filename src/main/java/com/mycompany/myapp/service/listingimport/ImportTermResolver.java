package com.mycompany.myapp.service.listingimport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Tra ngành nghề, tỉnh/thành của dòng nhập → id. Nạp một lần cho cả đợt (2.394 ngành, 98 tỉnh), tra trong bộ nhớ.
 *
 * <ul>
 *   <li>Ngành: mã VSIC (đuôi slug, vd. {@code ...-4690}), slug, hoặc tên (không phân biệt dấu, hoa thường).</li>
 *   <li>Tỉnh: tên ("Hà Nội", "TP. Hồ Chí Minh", "Tỉnh Bắc Ninh"), mã (01, 79), slug. Ưu tiên bộ 34 tỉnh mới
 *       (từ 01/7/2025, slug đuôi {@code -2025}); không có thì bộ 63 tỉnh cũ.</li>
 * </ul>
 */
public final class ImportTermResolver {

    private final Map<String, Long> categoryByKey = new HashMap<>();
    private final Map<String, Long> provinceNew = new HashMap<>();
    private final Map<String, Long> provinceOld = new HashMap<>();

    public ImportTermResolver(DataSource dataSource) {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.query("select id, slug, name from category", rs -> {
            long id = rs.getLong("id");
            String slug = rs.getString("slug");
            if (slug != null) {
                categoryByKey.putIfAbsent("slug:" + slug, id);
                int dash = slug.lastIndexOf('-');
                String code = dash >= 0 ? slug.substring(dash + 1) : "";
                if (code.matches("\\d{2,6}")) {
                    categoryByKey.putIfAbsent("code:" + code, id);
                }
            }
            categoryByKey.putIfAbsent("name:" + ListingImportNormalizer.folded(rs.getString("name")), id);
        });
        jdbc.query("select id, slug, name, code from location where type = 'province'", rs -> {
            String slug = rs.getString("slug");
            Map<String, Long> target = slug != null && slug.endsWith("-2025") ? provinceNew : provinceOld;
            long id = rs.getLong("id");
            target.putIfAbsent(provinceKey(rs.getString("name")), id);
            if (slug != null) {
                target.putIfAbsent(slug, id);
            }
            String code = rs.getString("code") == null ? "" : rs.getString("code").replaceAll("\\D", "");
            if (!code.isEmpty()) {
                target.putIfAbsent("code:" + Integer.parseInt(code), id);
            }
        });
    }

    /** Cách viết tắt hay gặp → tên chuẩn (đã bỏ dấu, bỏ tiền tố). */
    private static final Map<String, String> PROVINCE_ALIASES = Map.of(
        "hcm",
        "ho chi minh",
        "tphcm",
        "ho chi minh",
        "sai gon",
        "ho chi minh",
        "saigon",
        "ho chi minh",
        "hn",
        "ha noi",
        "tphn",
        "ha noi",
        "hue",
        "hue",
        "thua thien hue",
        "hue"
    );

    /** Tên tỉnh bỏ tiền tố: "Thành phố Hà Nội", "TP. Hà Nội", "Hà Nội" → {@code ha noi}; "TP.HCM" → {@code ho chi minh}. */
    static String provinceKey(String name) {
        String k = ListingImportNormalizer.folded(name).replaceFirst("^(thanh pho|tp|tinh)\\s+", "");
        return PROVINCE_ALIASES.getOrDefault(k, k);
    }

    /**
     * Ngành nghề từ ô "categories" (nhiều giá trị cách nhau bởi ; | , xuống dòng) và ô mã ngành chính.
     * @param unknown nhận các giá trị không tra được (để ghi chú)
     */
    public Set<Long> categories(String categories, String industryCode, List<String> unknown) {
        Set<Long> ids = new LinkedHashSet<>();
        List<String> values = new ArrayList<>();
        if (categories != null) {
            values.addAll(List.of(categories.split("[;|\\n]+|,(?=\\s*\\S)")));
        }
        if (industryCode != null) {
            values.add(industryCode);
        }
        for (String raw : values) {
            String v = ListingImportNormalizer.clean(raw);
            if (v == null) {
                continue;
            }
            Long id = category(v);
            if (id != null) {
                ids.add(id);
            } else if (unknown != null) {
                unknown.add(v);
            }
        }
        return ids;
    }

    private Long category(String v) {
        // "4690", "4690 - Bán buôn tổng hợp", "Bán buôn tổng hợp (4690)": lấy mã nếu có
        java.util.regex.Matcher code = java.util.regex.Pattern.compile("(?<!\\d)(\\d{4,5})(?!\\d)").matcher(v);
        if (code.find()) {
            Long id = categoryByKey.get("code:" + code.group(1));
            if (id != null) {
                return id;
            }
        }
        Long bySlug = categoryByKey.get("slug:" + v.toLowerCase());
        if (bySlug != null) {
            return bySlug;
        }
        String name = ListingImportNormalizer.folded(v.replaceAll("^\\d+\\s*[-:.]\\s*|\\s*\\(\\d+\\)$", ""));
        return categoryByKey.get("name:" + name);
    }

    /** Tỉnh/thành; null nếu không tra được. */
    public Long province(String value) {
        String v = ListingImportNormalizer.clean(value);
        if (v == null) {
            return null;
        }
        String key = v.matches("\\d{1,2}")
            ? "code:" + Integer.parseInt(v)
            : v.contains("-") && !v.contains(" ")
              ? v.toLowerCase()
              : provinceKey(v);
        Long id = provinceNew.get(key);
        return id != null ? id : provinceOld.get(key);
    }

    /** Tỉnh lấy từ cuối địa chỉ khi không có cột tỉnh: "..., Phường X, TP Hồ Chí Minh" → TP Hồ Chí Minh. */
    public Long provinceFromAddress(String address) {
        String a = ListingImportNormalizer.clean(address);
        if (a == null) {
            return null;
        }
        String[] parts = a.split(",");
        for (int i = parts.length - 1; i >= Math.max(0, parts.length - 2); i--) {
            String p = parts[i].trim();
            if (p.equalsIgnoreCase("Việt Nam") || p.equalsIgnoreCase("Viet Nam") || p.equalsIgnoreCase("VN")) {
                continue;
            }
            Long id = province(p);
            if (id != null) {
                return id;
            }
        }
        return null;
    }
}
