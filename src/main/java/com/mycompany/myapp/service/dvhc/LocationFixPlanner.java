package com.mycompany.myapp.service.dvhc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Lập kế hoạch sửa bảng {@code location} theo danh mục đơn vị hành chính chính thức (bảng {@code location_conversion}).
 * Hàm thuần, không đụng DB, để kiểm thử và chạy thử (dry-run).
 * <ul>
 *   <li>Bộ cũ: điền cột {@code code} từ mã trong slug.</li>
 *   <li>Bộ mới, tỉnh: mã từ slug; tên theo danh mục.</li>
 *   <li>Bộ mới, xã: khớp với danh mục <b>theo tên trong cùng tỉnh trước</b> (DB có chỗ gán lẫn mã giữa các xã: Kim Liên mang
 *   mã của Văn Miếu - Quốc Tử Giám), rồi theo mã cho xã còn lại (đổi tên). Xã khớp được: sửa mã, tên, slug, {@code type = ward},
 *   tỉnh cha. Xã chính thức chưa có: thêm mới. Xã trong DB không khớp: chỉ báo cáo, không xóa (có thể đang gắn doanh nghiệp).</li>
 * </ul>
 */
public final class LocationFixPlanner {

    /** Một dòng {@code location} hiện có. */
    public record LocationRow(long id, String name, String slug, String type, String code, Long parentId) {}

    /** Một đơn vị trong danh mục chính thức. {@code provinceCode} null với tỉnh. */
    public record OfficialUnit(String provinceCode, String code, String name) {}

    /** Giá trị mới cho một dòng có sẵn. */
    public record Update(long id, String name, String slug, String type, String code, Long parentId) {}

    /** Xã mới cần thêm, dưới tỉnh mới {@code parentId}. */
    public record Insert(String name, String slug, String type, String code, long parentId) {}

    /**
     * @param renames tất cả xã đổi tên khi khớp theo mã, và xã thêm mới (luôn liệt kê đủ để người dùng xem lại).
     * @param samples ví dụ các thay đổi khác (tối đa {@value #MAX_SAMPLES}).
     */
    public record Plan(
        List<Update> updates,
        List<Insert> inserts,
        Map<String, Integer> stats,
        List<String> unmatched,
        List<String> renames,
        List<String> samples
    ) {
        public boolean isEmpty() {
            return updates.isEmpty() && inserts.isEmpty();
        }
    }

    private static final int MAX_SAMPLES = 40;

    private LocationFixPlanner() {}

    public static Plan plan(List<LocationRow> rows, List<OfficialUnit> provinces, List<OfficialUnit> wards) {
        Map<String, Integer> stats = new LinkedHashMap<>();
        List<Update> updates = new ArrayList<>();
        List<Insert> inserts = new ArrayList<>();
        List<String> unmatched = new ArrayList<>();
        List<String> renames = new ArrayList<>();
        List<String> samples = new ArrayList<>();
        Map<Long, LocationRow> byId = new HashMap<>();
        rows.forEach(r -> byId.put(r.id(), r));

        // ---- Bộ cũ: điền mã từ slug ----
        for (LocationRow row : rows) {
            if (AdministrativeUnits.isNew(row.slug())) {
                continue;
            }
            String code = AdministrativeUnits.codeFromSlug(row.slug());
            if (code != null && !code.equals(row.code())) {
                updates.add(new Update(row.id(), row.name(), row.slug(), row.type(), code, row.parentId()));
                count(stats, "Bộ cũ: điền mã");
            }
        }

        // ---- Bộ mới: tỉnh ----
        Map<String, OfficialUnit> officialProvinces = new HashMap<>();
        provinces.forEach(p -> officialProvinces.put(p.code(), p));
        Map<String, Long> newProvinceIdByCode = new HashMap<>();
        Map<Long, String> newProvinceCodeById = new HashMap<>();
        for (LocationRow row : rows) {
            if (!AdministrativeUnits.isNew(row.slug()) || row.parentId() != null) {
                continue;
            }
            String code = AdministrativeUnits.codeFromSlug(row.slug());
            OfficialUnit official = officialProvinces.get(code);
            if (official == null) {
                unmatched.add("Tỉnh mới không có trong danh mục: " + describe(row));
                continue;
            }
            newProvinceIdByCode.put(code, row.id());
            newProvinceCodeById.put(row.id(), code);
            if (
                !Objects.equals(official.name(), row.name()) || !code.equals(row.code()) || !AdministrativeUnits.PROVINCE.equals(row.type())
            ) {
                updates.add(new Update(row.id(), official.name(), row.slug(), AdministrativeUnits.PROVINCE, code, null));
                count(stats, "Tỉnh mới: sửa mã/tên");
                if (!Objects.equals(official.name(), row.name())) {
                    sample(samples, "Đổi tên tỉnh: \"" + row.name() + "\" → \"" + official.name() + "\"");
                }
            }
        }
        for (OfficialUnit province : provinces) {
            if (!newProvinceIdByCode.containsKey(province.code())) {
                unmatched.add("Tỉnh mới trong danh mục chưa có trong DB: " + province.code() + " " + province.name());
            }
        }

        // ---- Bộ mới: xã, khớp theo từng tỉnh ----
        Map<String, List<LocationRow>> dbWardsByProvince = new HashMap<>();
        for (LocationRow row : rows) {
            if (!AdministrativeUnits.isNew(row.slug()) || row.parentId() == null) {
                continue;
            }
            String provinceCode = newProvinceCodeById.get(row.parentId());
            if (provinceCode == null) {
                unmatched.add("Xã mới không nằm dưới tỉnh mới hợp lệ: " + describe(row));
                continue;
            }
            dbWardsByProvince.computeIfAbsent(provinceCode, k -> new ArrayList<>()).add(row);
        }
        Map<String, List<OfficialUnit>> officialWardsByProvince = new HashMap<>();
        wards.forEach(w -> officialWardsByProvince.computeIfAbsent(w.provinceCode(), k -> new ArrayList<>()).add(w));

        for (Map.Entry<String, List<OfficialUnit>> entry : officialWardsByProvince.entrySet()) {
            String provinceCode = entry.getKey();
            Long provinceId = newProvinceIdByCode.get(provinceCode);
            if (provinceId == null) {
                continue; // đã báo ở phần tỉnh
            }
            List<OfficialUnit> officialLeft = new ArrayList<>(entry.getValue());
            List<LocationRow> dbLeft = new ArrayList<>(dbWardsByProvince.getOrDefault(provinceCode, List.of()));
            Map<LocationRow, OfficialUnit> matches = new LinkedHashMap<>();

            // Lượt 1: tên (chỉ khi tên duy nhất ở cả hai phía trong tỉnh)
            Map<String, List<OfficialUnit>> officialByName = group(officialLeft, o -> AdministrativeUnits.nameKey(o.name()));
            Map<String, List<LocationRow>> dbByName = group(dbLeft, r -> AdministrativeUnits.nameKey(r.name()));
            for (Map.Entry<String, List<LocationRow>> db : dbByName.entrySet()) {
                List<OfficialUnit> candidates = officialByName.get(db.getKey());
                if (db.getValue().size() == 1 && candidates != null && candidates.size() == 1) {
                    matches.put(db.getValue().get(0), candidates.get(0));
                    count(stats, "Xã mới: khớp theo tên");
                }
            }
            dbLeft.removeAll(matches.keySet());
            officialLeft.removeAll(matches.values());

            // Lượt 2: mã (xã đổi tên)
            for (LocationRow row : List.copyOf(dbLeft)) {
                String code = AdministrativeUnits.codeFromSlug(row.slug());
                Optional<OfficialUnit> byCode = officialLeft
                    .stream()
                    .filter(o -> o.code().equals(code))
                    .findFirst();
                if (byCode.isPresent()) {
                    matches.put(row, byCode.get());
                    dbLeft.remove(row);
                    officialLeft.remove(byCode.get());
                    count(stats, "Xã mới: khớp theo mã (đổi tên)");
                    renames.add("Đổi tên xã (cùng mã " + code + "): \"" + row.name() + "\" → \"" + byCode.get().name() + "\"");
                }
            }

            for (Map.Entry<LocationRow, OfficialUnit> match : matches.entrySet()) {
                LocationRow row = match.getKey();
                OfficialUnit official = match.getValue();
                String slug = AdministrativeUnits.newSlug(official.name(), official.code());
                boolean codeChanged = !official.code().equals(AdministrativeUnits.codeFromSlug(row.slug()));
                boolean changed =
                    codeChanged ||
                    !official.code().equals(row.code()) ||
                    !official.name().equals(row.name()) ||
                    !slug.equals(row.slug()) ||
                    !AdministrativeUnits.WARD.equals(row.type()) ||
                    !provinceId.equals(row.parentId());
                if (!changed) {
                    continue;
                }
                updates.add(new Update(row.id(), official.name(), slug, AdministrativeUnits.WARD, official.code(), provinceId));
                if (codeChanged) {
                    count(stats, "Xã mới: sửa mã sai");
                    sample(
                        samples,
                        "Sửa mã: \"" + row.name() + "\" " + AdministrativeUnits.codeFromSlug(row.slug()) + " → " + official.code()
                    );
                }
                if (!official.name().equals(row.name())) {
                    count(stats, "Xã mới: sửa tên");
                }
                if (!slug.equals(row.slug())) {
                    count(stats, "Xã mới: đổi slug");
                }
                if (!AdministrativeUnits.WARD.equals(row.type())) {
                    count(stats, "Xã mới: type → ward");
                }
            }
            for (OfficialUnit official : officialLeft) {
                inserts.add(
                    new Insert(
                        official.name(),
                        AdministrativeUnits.newSlug(official.name(), official.code()),
                        AdministrativeUnits.WARD,
                        official.code(),
                        provinceId
                    )
                );
                count(stats, "Xã mới: thêm (DB chưa có)");
                renames.add("Thêm xã: " + official.code() + " " + official.name() + " (tỉnh " + provinceCode + ")");
            }
            for (LocationRow row : dbLeft) {
                unmatched.add("Xã mới trong DB không có trong danh mục: " + describe(row));
            }
        }
        for (Map.Entry<String, List<LocationRow>> entry : dbWardsByProvince.entrySet()) {
            if (!officialWardsByProvince.containsKey(entry.getKey())) {
                entry.getValue().forEach(row -> unmatched.add("Xã mới thuộc tỉnh không có trong danh mục: " + describe(row)));
            }
        }
        // Gộp các cập nhật cùng id (không xảy ra trong thực tế: bộ cũ và bộ mới tách biệt), giữ bản sau cùng
        Map<Long, Update> merged = new LinkedHashMap<>();
        updates.forEach(u -> merged.put(u.id(), u));
        return new Plan(List.copyOf(merged.values()), inserts, stats, unmatched, renames, samples);
    }

    private static <T> Map<String, List<T>> group(List<T> items, java.util.function.Function<T, String> key) {
        Map<String, List<T>> result = new HashMap<>();
        items.forEach(item -> result.computeIfAbsent(key.apply(item), k -> new ArrayList<>()).add(item));
        return result;
    }

    private static void count(Map<String, Integer> stats, String key) {
        stats.merge(key, 1, Integer::sum);
    }

    private static void sample(List<String> samples, String text) {
        if (samples.size() < MAX_SAMPLES) {
            samples.add(text);
        }
    }

    private static String describe(LocationRow row) {
        return row.id() + " " + row.name() + " (" + row.slug() + ")";
    }
}
