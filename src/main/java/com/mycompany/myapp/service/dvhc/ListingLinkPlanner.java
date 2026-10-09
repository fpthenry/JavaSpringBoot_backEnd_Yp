package com.mycompany.myapp.service.dvhc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Quyết định liên kết địa phương cần thêm cho một doanh nghiệp, để tra cứu được theo cả địa giới mới (từ 01/7/2025) và cũ.
 * Hàm thuần, không đụng DB. Chỉ <b>thêm</b> liên kết, không xóa liên kết nào.
 * <p>
 * <b>Đơn vị mới</b> (khi doanh nghiệp chưa gắn đơn vị mới nào), lấy theo thứ tự:
 * <ol>
 *   <li>mã xã mới trong {@code location_json} (dữ liệu nguồn đã ghi theo địa giới mới);</li>
 *   <li>mã tỉnh mới trong {@code location_json};</li>
 *   <li>liên kết cũ qua bảng chuyển đổi: xã cũ về đúng một xã mới → xã mới; xã cũ nhập một phần vào nhiều xã cùng tỉnh →
 *   tỉnh mới; huyện cũ → xã mới nếu chỉ một (đặc khu), không thì tỉnh mới; tỉnh cũ → tỉnh mới.</li>
 * </ol>
 * <b>Đơn vị cũ</b> (khi doanh nghiệp chưa gắn đơn vị cũ nào, ví dụ chỉ gắn xã mới): từ xã mới, nếu mọi xã cũ hợp thành thuộc
 * một huyện cũ thì gắn huyện đó, thuộc một tỉnh cũ thì gắn tỉnh đó; nhiều hơn thì bỏ qua (không đoán).
 */
public final class ListingLinkPlanner {

    private static final Pattern PROVINCE_CODE = Pattern.compile("\"province_code\"\\s*:\\s*\"(\\d{2})\"");
    private static final Pattern WARD_CODE = Pattern.compile("\"ward_code\"\\s*:\\s*\"(\\d{5})\"");

    /** Một đơn vị trong bảng location. */
    public record Unit(long id, boolean isNew, String type, String code, String provinceCode) {}

    /** Kết quả cho một doanh nghiệp. {@code jsonAgreesWithOldWard}: null nếu không so được. */
    public record Decision(List<Long> add, String newSource, String oldSource, Boolean jsonAgreesWithOldWard) {}

    /** Bảng tra cứu, dựng một lần cho cả job. */
    public static final class Context {

        final Map<Long, Unit> unitsById = new HashMap<>();
        final Map<String, Unit> newProvinces = new HashMap<>();
        final Map<String, Unit> newWards = new HashMap<>();
        final Map<String, Unit> oldProvinces = new HashMap<>();
        final Map<String, Unit> oldDistricts = new HashMap<>();
        final Map<String, Set<String>> newWardsByOldWard = new HashMap<>();
        final Map<String, Set<String>> newWardsByOldDistrict = new HashMap<>();
        final Map<String, Set<String>> newProvincesByOldProvince = new HashMap<>();
        final Map<String, Set<String>> oldDistrictsByNewWard = new HashMap<>();
        final Map<String, Set<String>> oldProvincesByNewWard = new HashMap<>();
        final Map<String, Set<String>> oldProvincesByNewProvince = new HashMap<>();
        final Map<String, String> provinceOfNewWard = new HashMap<>();

        public void addUnit(Unit unit) {
            unitsById.put(unit.id(), unit);
            if (unit.code() == null) {
                return;
            }
            if (unit.isNew()) {
                if (AdministrativeUnits.PROVINCE.equals(unit.type())) {
                    newProvinces.put(unit.code(), unit);
                } else if (AdministrativeUnits.WARD.equals(unit.type())) {
                    newWards.put(unit.code(), unit);
                }
            } else if (AdministrativeUnits.PROVINCE.equals(unit.type())) {
                oldProvinces.put(unit.code(), unit);
            } else if (AdministrativeUnits.DISTRICT.equals(unit.type())) {
                oldDistricts.put(unit.code(), unit);
            }
        }

        /** Một dòng bảng chuyển đổi. {@code oldWardCode} null với dòng cấp huyện (cả huyện thành đặc khu). */
        public void addConversion(
            String newProvinceCode,
            String newWardCode,
            String oldWardCode,
            String oldDistrictCode,
            String oldProvinceCode
        ) {
            if (oldWardCode != null) {
                newWardsByOldWard.computeIfAbsent(oldWardCode, k -> new HashSet<>()).add(newWardCode);
            }
            newWardsByOldDistrict.computeIfAbsent(oldDistrictCode, k -> new HashSet<>()).add(newWardCode);
            newProvincesByOldProvince.computeIfAbsent(oldProvinceCode, k -> new HashSet<>()).add(newProvinceCode);
            oldDistrictsByNewWard.computeIfAbsent(newWardCode, k -> new HashSet<>()).add(oldDistrictCode);
            oldProvincesByNewWard.computeIfAbsent(newWardCode, k -> new HashSet<>()).add(oldProvinceCode);
            oldProvincesByNewProvince.computeIfAbsent(newProvinceCode, k -> new HashSet<>()).add(oldProvinceCode);
            provinceOfNewWard.put(newWardCode, newProvinceCode);
        }
    }

    private ListingLinkPlanner() {}

    /** Mã tỉnh, mã xã trong location_json; đọc bằng regex để dùng được cả JSON hỏng (7 dòng). */
    public static String[] jsonCodes(String locationJson) {
        if (locationJson == null) {
            return new String[] { null, null };
        }
        Matcher province = PROVINCE_CODE.matcher(locationJson);
        Matcher ward = WARD_CODE.matcher(locationJson);
        return new String[] { province.find() ? province.group(1) : null, ward.find() ? ward.group(1) : null };
    }

    public static Decision plan(Context ctx, String jsonProvinceCode, String jsonWardCode, long[] currentLocationIds) {
        List<Unit> current = new ArrayList<>();
        for (long id : currentLocationIds) {
            Unit unit = ctx.unitsById.get(id);
            if (unit != null) {
                current.add(unit);
            }
        }
        boolean hasNew = current.stream().anyMatch(Unit::isNew);
        boolean hasOld = current.stream().anyMatch(u -> !u.isNew());
        List<Long> add = new ArrayList<>();

        // ---- Đơn vị mới ----
        Unit newTarget = null;
        String newSource;
        if (hasNew) {
            newSource = "đã có đơn vị mới";
        } else {
            Unit jsonWard = jsonWardCode == null ? null : ctx.newWards.get(jsonWardCode);
            if (jsonWard != null && (jsonProvinceCode == null || jsonProvinceCode.equals(jsonWard.provinceCode()))) {
                newTarget = jsonWard;
                newSource = "JSON: xã mới";
            } else if (jsonProvinceCode != null && ctx.newProvinces.containsKey(jsonProvinceCode)) {
                newTarget = ctx.newProvinces.get(jsonProvinceCode);
                newSource = "JSON: tỉnh mới";
            } else {
                newTarget = fromOldLinks(ctx, current);
                newSource =
                    newTarget == null
                        ? hasOld
                            ? "không chuyển được (liên kết cũ không có trong bảng chuyển đổi)"
                            : "không có nguồn"
                        : "từ liên kết cũ: " + (AdministrativeUnits.WARD.equals(newTarget.type()) ? "xã mới" : "tỉnh mới");
            }
            if (newTarget != null) {
                add.add(newTarget.id());
            }
        }

        // ---- So khớp JSON với xã cũ đang gắn ----
        Boolean agrees = null;
        if (jsonWardCode != null && ctx.newWards.containsKey(jsonWardCode)) {
            for (Unit unit : current) {
                if (!unit.isNew() && AdministrativeUnits.WARD.equals(unit.type()) && ctx.newWardsByOldWard.containsKey(unit.code())) {
                    agrees = ctx.newWardsByOldWard.get(unit.code()).contains(jsonWardCode);
                }
            }
        }

        // ---- Đơn vị cũ (chỉ khi chưa có) ----
        String oldSource = hasOld ? "đã có đơn vị cũ" : null;
        if (!hasOld) {
            Unit newUnit = newTarget != null ? newTarget : current.stream().filter(Unit::isNew).findFirst().orElse(null);
            Unit oldTarget = null;
            if (newUnit != null && AdministrativeUnits.WARD.equals(newUnit.type())) {
                Set<String> districts = ctx.oldDistrictsByNewWard.getOrDefault(newUnit.code(), Set.of());
                Set<String> provinces = ctx.oldProvincesByNewWard.getOrDefault(newUnit.code(), Set.of());
                if (districts.size() == 1 && ctx.oldDistricts.containsKey(districts.iterator().next())) {
                    oldTarget = ctx.oldDistricts.get(districts.iterator().next());
                } else if (provinces.size() == 1) {
                    oldTarget = ctx.oldProvinces.get(provinces.iterator().next());
                }
            } else if (newUnit != null) {
                Set<String> provinces = ctx.oldProvincesByNewProvince.getOrDefault(newUnit.code(), Set.of());
                if (provinces.size() == 1) {
                    oldTarget = ctx.oldProvinces.get(provinces.iterator().next());
                }
            }
            if (oldTarget != null) {
                add.add(oldTarget.id());
                oldSource = "thêm " + (AdministrativeUnits.DISTRICT.equals(oldTarget.type()) ? "huyện cũ" : "tỉnh cũ");
            } else {
                oldSource = newUnit == null ? "không có nguồn" : "không xác định được (gộp từ nhiều huyện/tỉnh cũ)";
            }
        }
        return new Decision(add, newSource, oldSource, agrees);
    }

    /** Đơn vị mới suy từ liên kết cũ, chọn đơn vị cụ thể nhất: xã > huyện > tỉnh. */
    private static Unit fromOldLinks(Context ctx, List<Unit> current) {
        Unit best = null;
        int bestRank = -1;
        for (Unit unit : current) {
            if (unit.isNew() || unit.code() == null) {
                continue;
            }
            Unit candidate = null;
            int rank = 0;
            if (AdministrativeUnits.WARD.equals(unit.type())) {
                candidate = wardOrProvince(ctx, ctx.newWardsByOldWard.get(unit.code()));
                rank = 3;
            } else if (AdministrativeUnits.DISTRICT.equals(unit.type())) {
                candidate = wardOrProvince(ctx, ctx.newWardsByOldDistrict.get(unit.code()));
                rank = 2;
            } else if (AdministrativeUnits.PROVINCE.equals(unit.type())) {
                Set<String> provinces = ctx.newProvincesByOldProvince.getOrDefault(unit.code(), Set.of());
                candidate = provinces.size() == 1 ? ctx.newProvinces.get(provinces.iterator().next()) : null;
                rank = 1;
            }
            if (candidate != null && rank > bestRank) {
                best = candidate;
                bestRank = rank;
            }
        }
        return best;
    }

    /** Một xã mới duy nhất → xã đó; nhiều xã cùng một tỉnh mới → tỉnh đó; khác tỉnh → null. */
    private static Unit wardOrProvince(Context ctx, Set<String> newWardCodes) {
        if (newWardCodes == null || newWardCodes.isEmpty()) {
            return null;
        }
        if (newWardCodes.size() == 1) {
            return ctx.newWards.get(newWardCodes.iterator().next());
        }
        Set<String> provinces = new HashSet<>();
        newWardCodes.forEach(code -> provinces.add(ctx.provinceOfNewWard.get(code)));
        return provinces.size() == 1 ? ctx.newProvinces.get(provinces.iterator().next()) : null;
    }
}
