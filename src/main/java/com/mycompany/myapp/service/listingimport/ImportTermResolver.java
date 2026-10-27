package com.mycompany.myapp.service.listingimport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Tra ngành nghề, địa phương của dòng nhập → id. Nạp một lần cho cả đợt (2.394 ngành, ~15.300 địa phương,
 * bảng chuyển đổi địa giới 10.571 dòng), tra trong bộ nhớ.
 *
 * <ul>
 *   <li>Ngành: mã VSIC (đuôi slug, vd. {@code ...-4690}), slug, hoặc tên (không phân biệt dấu, hoa thường).</li>
 *   <li>Tỉnh: tên ("Hà Nội", "TP. Hồ Chí Minh", "Tỉnh Bắc Ninh"), mã (01, 79), slug. Ưu tiên bộ 34 tỉnh mới
 *       (từ 01/7/2025, slug đuôi {@code -2025}); không có thì bộ 63 tỉnh cũ.</li>
 *   <li>Phường/xã: tên đầy đủ ("Phường Ba Đình") rồi tên rút gọn ("Ba Đình"), hoặc mã ("00004"); chỉ tìm TRONG tỉnh của
 *       dòng (14.501 phường/xã chỉ có 8.661 tên khác nhau) và tỉnh cũ/mới tương ứng. Ưu tiên xã mới; xã cũ mà bảng
 *       chuyển đổi cho ra đúng một xã mới thì trả thêm xã mới (doanh nghiệp đang có cũng gắn cả hai).</li>
 * </ul>
 */
public final class ImportTermResolver {

    private final Map<String, Long> categoryByKey = new HashMap<>();
    private final Map<String, Long> provinceNew = new HashMap<>();
    private final Map<String, Long> provinceOld = new HashMap<>();

    /** Tỉnh, quận/huyện, phường/xã: id → thông tin. */
    private final Map<Long, Unit> units = new HashMap<>();
    /** Tỉnh → khóa tên ("full:phuong ba dinh", "bare:ba dinh", "code:00004") → phường/xã. */
    private final Map<Long, Map<String, List<Long>>> wardsByProvince = new HashMap<>();
    private final Map<Long, Map<String, List<Long>>> districtsByProvince = new HashMap<>();
    /** Tỉnh cũ ↔ tỉnh mới tương ứng (theo mã trong bảng chuyển đổi). */
    private final Map<Long, Set<Long>> provinceCounterparts = new HashMap<>();
    /** Mã xã mới → id; mã xã cũ → mã xã mới (chỉ khi xã cũ về đúng MỘT xã mới). */
    private final Map<String, Long> newWardByCode = new HashMap<>();
    private final Map<String, String> oldToNewWardCode = new HashMap<>();

    /** Một đơn vị hành chính. {@code province} = id tỉnh chứa nó (chính nó nếu là tỉnh); {@code parent} = cha trực tiếp. */
    public record Unit(long id, String name, String type, String code, boolean isNew, long province, Long parent) {}

    /** Kết quả tìm phường/xã: xã tìm được, xã mới tương ứng (nếu xã tìm được là xã cũ), hoặc ghi chú vì sao không gắn. */
    public record WardMatch(Unit ward, Unit newWard, String note) {
        /** Xã dùng để ghi location_json: xã mới nếu có (như dữ liệu đang có), không thì xã tìm được. */
        public Unit display() {
            return newWard != null ? newWard : ward;
        }
    }

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

        // Địa phương: tỉnh, quận/huyện, phường/xã (parent_id trỏ tới id cha)
        record Raw(long id, String name, String slug, String type, String code, Long parent) {}
        List<Raw> raws = new ArrayList<>();
        jdbc.query("select id, name, slug, type, code, parent_id from location where type in ('province', 'district', 'ward')", rs -> {
            long parentValue = rs.getLong("parent_id");
            Long parent = rs.wasNull() ? null : parentValue; // wasNull() chỉ đúng ngay sau lần đọc parent_id
            raws.add(
                new Raw(rs.getLong("id"), rs.getString("name"), rs.getString("slug"), rs.getString("type"), rs.getString("code"), parent)
            );
        });
        Map<Long, Raw> byId = new HashMap<>();
        raws.forEach(r -> byId.put(r.id(), r));
        Map<String, Long> oldProvinceByCode = new HashMap<>();
        Map<String, Long> newProvinceByCode = new HashMap<>();
        for (Raw r : raws) {
            boolean isNew = r.slug() != null && r.slug().endsWith("-2025");
            // tỉnh chứa: đi lên theo parent tới cấp tỉnh (tối đa 3 bậc)
            Raw p = r;
            for (int i = 0; i < 3 && p != null && !"province".equals(p.type()); i++) {
                p = p.parent() == null ? null : byId.get(p.parent());
            }
            if (p == null) {
                continue;
            }
            Unit u = new Unit(r.id(), r.name(), r.type(), r.code(), isNew, p.id(), r.parent());
            units.put(r.id(), u);
            switch (r.type()) {
                case "province" -> {
                    Map<String, Long> target = isNew ? provinceNew : provinceOld;
                    target.putIfAbsent(provinceKey(r.name()), r.id());
                    if (r.slug() != null) {
                        target.putIfAbsent(r.slug(), r.id());
                    }
                    String code = r.code() == null ? "" : r.code().replaceAll("\\D", "");
                    if (!code.isEmpty()) {
                        target.putIfAbsent("code:" + Integer.parseInt(code), r.id());
                        (isNew ? newProvinceByCode : oldProvinceByCode).putIfAbsent(code, r.id());
                    }
                }
                case "ward" -> {
                    index(wardsByProvince, p.id(), unitKeys(r.name(), WARD_PREFIX), r.code(), r.id());
                    if (isNew && r.code() != null) {
                        newWardByCode.putIfAbsent(r.code(), r.id());
                    }
                }
                default -> index(districtsByProvince, p.id(), unitKeys(r.name(), DISTRICT_PREFIX), r.code(), r.id());
            }
        }

        // Bảng chuyển đổi: tỉnh cũ ↔ tỉnh mới; xã cũ → xã mới (chỉ khi về đúng một xã mới)
        Map<String, Set<String>> newCodesOfOldWard = new HashMap<>();
        jdbc.query("select old_province_code, new_province_code, old_ward_code, new_ward_code from location_conversion", rs -> {
            Long o = oldProvinceByCode.get(rs.getString("old_province_code"));
            Long n = newProvinceByCode.get(rs.getString("new_province_code"));
            if (o != null && n != null) {
                provinceCounterparts.computeIfAbsent(o, k -> new HashSet<>()).add(n);
                provinceCounterparts.computeIfAbsent(n, k -> new HashSet<>()).add(o);
            }
            String oldWard = rs.getString("old_ward_code");
            String newWard = rs.getString("new_ward_code");
            if (oldWard != null && newWard != null) {
                newCodesOfOldWard.computeIfAbsent(oldWard, k -> new HashSet<>()).add(newWard);
            }
        });
        newCodesOfOldWard.forEach((old, news) -> {
            if (news.size() == 1) {
                oldToNewWardCode.put(old, news.iterator().next());
            }
        });
    }

    private static final String WARD_PREFIX = "^(phuong|xa|thi tran|dac khu|p|x|tt)\\s+";
    private static final String DISTRICT_PREFIX = "^(quan|huyen|thi xa|thanh pho|tp|q|h|tx)\\s+";

    /** Khóa tra tên đơn vị: tên đầy đủ không dấu và tên bỏ tiền tố ("Phường Ba Đình" → "phuong ba dinh", "ba dinh"). */
    static String[] unitKeys(String name, String prefix) {
        String full = ListingImportNormalizer.folded(name);
        String bare = full.replaceFirst(prefix, "");
        return new String[] { "full:" + full, "bare:" + bare };
    }

    private static void index(Map<Long, Map<String, List<Long>>> target, long province, String[] keys, String code, long id) {
        Map<String, List<Long>> m = target.computeIfAbsent(province, k -> new HashMap<>());
        for (String k : keys) {
            m.computeIfAbsent(k, x -> new ArrayList<>()).add(id);
        }
        if (code != null && !code.isBlank()) {
            m.computeIfAbsent("code:" + code.trim(), x -> new ArrayList<>()).add(id);
        }
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

    /** Thông tin một địa phương (tỉnh, quận/huyện, phường/xã) theo id; null nếu không có. */
    public Unit unit(Long id) {
        return id == null ? null : units.get(id);
    }

    /**
     * Phường/xã trong tỉnh {@code provinceId} (và tỉnh cũ/mới tương ứng). {@code district} (không bắt buộc) dùng phân biệt
     * xã cũ trùng tên. Trả null khi ô trống; {@link WardMatch#note()} khi không gắn được (không rõ tỉnh, không thấy, trùng tên).
     */
    public WardMatch ward(String value, Long provinceId, String district) {
        String v = ListingImportNormalizer.clean(value);
        if (v == null) {
            return null;
        }
        if (provinceId == null) {
            return new WardMatch(null, null, "Không gắn phường/xã \"" + v + "\" vì không rõ tỉnh (14.501 phường/xã chỉ có 8.661 tên)");
        }
        Set<Long> provinces = new LinkedHashSet<>();
        provinces.add(provinceId);
        provinces.addAll(provinceCounterparts.getOrDefault(provinceId, Set.of()));

        List<String> keys = v.matches("\\d{4,5}") ? List.of("code:" + (v.length() == 4 ? "0" + v : v)) : List.of(unitKeys(v, WARD_PREFIX));
        List<Long> found = new ArrayList<>();
        for (String key : keys) {
            for (Long p : provinces) {
                found.addAll(wardsByProvince.getOrDefault(p, Map.of()).getOrDefault(key, List.of()));
            }
            if (!found.isEmpty()) {
                break; // có kết quả theo tên đầy đủ thì không xét tên rút gọn
            }
        }
        if (found.isEmpty()) {
            return new WardMatch(null, null, "Không thấy phường/xã \"" + v + "\" trong tỉnh");
        }
        List<Unit> candidates = new ArrayList<>(found.stream().distinct().map(units::get).toList());
        // Quận/huyện (địa giới cũ) phân biệt xã cũ trùng tên trong tỉnh
        String d = ListingImportNormalizer.clean(district);
        if (candidates.size() > 1 && d != null) {
            Set<Long> districts = new HashSet<>();
            for (Long p : provinces) {
                Map<String, List<Long>> m = districtsByProvince.getOrDefault(p, Map.of());
                for (String k : unitKeys(d, DISTRICT_PREFIX)) {
                    districts.addAll(m.getOrDefault(k, List.of()));
                }
            }
            List<Unit> inDistrict = candidates
                .stream()
                .filter(u -> !u.isNew() && districts.contains(u.parent()))
                .toList();
            if (!inDistrict.isEmpty()) {
                candidates = new ArrayList<>(inDistrict);
            }
        }
        if (candidates.size() > 1 && candidates.stream().anyMatch(Unit::isNew)) {
            candidates = new ArrayList<>(candidates.stream().filter(Unit::isNew).toList());
        }
        if (candidates.size() > 1) {
            return new WardMatch(
                null,
                null,
                "Có " + candidates.size() + " phường/xã tên \"" + v + "\" trong tỉnh, không gắn (thêm cột quận/huyện)"
            );
        }
        Unit ward = candidates.get(0);
        Unit newWard = null;
        if (!ward.isNew() && ward.code() != null) {
            String newCode = oldToNewWardCode.get(ward.code());
            newWard = newCode == null ? null : unit(newWardByCode.get(newCode));
        }
        return new WardMatch(ward, newWard, null);
    }
}
