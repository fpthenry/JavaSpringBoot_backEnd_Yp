package com.mycompany.myapp.service.dvhc;

import com.mycompany.myapp.repository.search.VietnameseText;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tạo địa chỉ theo địa giới hành chính mới (từ 01/7/2025) từ địa chỉ cũ: giữ phần số nhà, đường, khu; bỏ các đoạn hành
 * chính ở cuối (phường, quận, tỉnh, "Việt Nam"); ghép xã mới, tỉnh mới. Hàm thuần.
 * <p>
 * Ví dụ: "Số 12, ngõ 72 Phố Tân Ấp, Phường Phúc Xá, Quận Ba Đình, Thành phố Hà Nội, Việt Nam"
 * → "Số 12, ngõ 72 Phố Tân Ấp, Phường Hồng Hà, Thành phố Hà Nội".
 * <p>
 * Không dùng cột street của location_json vì bị cắt mất chi tiết ("P3, KHU B4B, ..." chỉ còn "P3").
 * Trả null (không tạo) khi: địa chỉ đã là địa chỉ mới (đuôi có xã mới, không có cấp quận/huyện); chưa biết xã mới;
 * địa chỉ rỗng.
 */
public final class NewAddressPlanner {

    /** Kết quả: địa chỉ mới (null nếu không tạo) và lý do, để thống kê. */
    public record Result(String addressNew, String outcome) {}

    public static final String CREATED = "tạo địa chỉ mới";
    public static final String ALREADY_NEW = "đã là địa chỉ mới";
    public static final String NO_NEW_WARD = "chưa xác định được xã mới";
    public static final String EMPTY = "địa chỉ rỗng";

    private static final Pattern WORD_PREFIX = Pattern.compile("^(phuong|xa|thi tran|dac khu|quan|huyen|thi xa|thanh pho|tinh)\\s+(?=\\S)");
    private static final Pattern ABBREVIATED_PREFIX = Pattern.compile("^(p|f|x|tt|q|h|tx|tp|t)\\s*\\.\\s*(?=\\S)|^(tp|tx|tt)\\s+(?=\\S)");
    /** Đường giao thông, không phải đơn vị hành chính: "Tỉnh lộ 10", "Xa lộ Hà Nội", "Quốc lộ 1A", "Huyện lộ 2". */
    private static final Pattern ROAD = Pattern.compile("^(tinh|xa|huyen|quoc)\\s+lo\\b");
    private static final Set<String> COUNTRY = Set.of("viet nam", "vietnam", "vn");
    private static final Set<String> WARD_LEVEL = Set.of("phuong", "xa", "thi tran", "dac khu", "p", "f", "x", "tt");
    private static final Set<String> DISTRICT_LEVEL = Set.of("quan", "huyen", "thi xa", "q", "h", "tx");
    private static final Set<String> CITY = Set.of("thanh pho", "tp");
    private static final Pattern NON_WORD = Pattern.compile("[^a-z0-9 ]+");
    private static final Pattern SPACES = Pattern.compile("\\s+");
    private static final Pattern TRAILING_DOTS = Pattern.compile("[.\\s]+$");
    /**
     * Đơn vị hành chính viết dính vào đoạn trước, thiếu dấu phẩy: "ĐINH BỘ LĨNH P.26", "KP MỸ HƯNG P.TÂN PHONG". Chỉ nhận
     * dạng viết tắt có dấu chấm, đứng sau khoảng trắng, để không cắt nhầm "P112", "Lô P".
     */
    private static final Pattern INLINE_ADMIN = Pattern.compile("\\s+(?:P|F|X|TT|Q|H|TX|TP)\\.\\s*\\S.*$", Pattern.CASE_INSENSITIVE);

    private NewAddressPlanner() {}

    /** Một đoạn địa chỉ đã phân tích: tiền tố (bỏ dấu, không có dấu chấm; rỗng nếu không có) và tên (bỏ dấu, bỏ tiền tố). */
    record Segment(String text, String prefix, String name) {}

    /**
     * @param address         địa chỉ hiện có.
     * @param newWardName     tên xã mới của doanh nghiệp (null nếu chưa biết).
     * @param newProvinceName tên tỉnh mới.
     * @param provinceNames   tên mọi tỉnh cũ và mới, đã {@link #key(String)} và bỏ tiền tố, kèm tên gọi tắt ("hcm", "tphcm").
     * @param districtNames   tên huyện cũ (đã key, bỏ tiền tố) của các tỉnh cũ liên quan, để nhận ra "Thanh Xuân", "TP. Bắc Ninh".
     * @param localWardNames  tên xã cũ đang gắn (đã key, bỏ tiền tố), để nhận ra tên xã không có tiền tố.
     */
    public static Result plan(
        String address,
        String newWardName,
        String newProvinceName,
        Set<String> provinceNames,
        Set<String> districtNames,
        Set<String> localWardNames
    ) {
        if (address == null || address.isBlank()) {
            return new Result(null, EMPTY);
        }
        if (newWardName == null || newProvinceName == null) {
            return new Result(null, NO_NEW_WARD);
        }
        // Địa chỉ không có dấu phẩy mà dùng " - " làm dấu phân cách ("82 Phước Lăng - Xã Tam Phước - Huyện Long Điền - ...").
        // Có dấu phẩy thì chỉ tách theo dấu phẩy, để không cắt "Bà Rịa - Vũng Tàu", "cầu Giẽ - Ninh Bình" ở giữa địa chỉ.
        String separator = address.contains(",") || address.contains(";") ? "[,;]" : "\\s+-\\s+";
        List<Segment> segments = Arrays.stream(address.split(separator))
            .map(s -> TRAILING_DOTS.matcher(s.trim()).replaceAll("").trim())
            .filter(s -> !s.isEmpty() && !s.equals("-"))
            .map(NewAddressPlanner::parse)
            .toList();
        String newWard = parse(newWardName).name();

        // Bỏ dần từ cuối các đoạn hành chính
        int keep = segments.size();
        while (keep > 0 && isAdministrative(segments.get(keep - 1), provinceNames, districtNames, localWardNames, newWard)) {
            keep--;
        }
        List<Segment> tail = segments.subList(keep, segments.size());
        boolean hasNewWard = tail
            .stream()
            .anyMatch(s -> s.name().equals(newWard) && (s.prefix().isEmpty() || WARD_LEVEL.contains(s.prefix())));
        boolean hasDistrict = false;
        for (int i = 0; i < tail.size(); i++) {
            if (isDistrictLevel(tail, i, provinceNames, districtNames)) {
                hasDistrict = true;
            }
        }
        if (hasNewWard && !hasDistrict) {
            return new Result(null, ALREADY_NEW);
        }

        List<String> parts = new ArrayList<>();
        segments.subList(0, keep).forEach(s -> parts.add(s.text()));
        if (!parts.isEmpty()) {
            String last = INLINE_ADMIN.matcher(parts.get(parts.size() - 1))
                .replaceFirst("")
                .trim();
            if (last.isEmpty()) {
                parts.remove(parts.size() - 1);
            } else {
                parts.set(parts.size() - 1, last);
            }
        }
        parts.add(newWardName.trim());
        parts.add(newProvinceName.trim());
        return new Result(String.join(", ", parts), CREATED);
    }

    static Segment parse(String text) {
        String folded = key(text);
        // Bỏ dấu chấm của tiền tố viết tắt nhưng giữ để nhận dạng ("p. thanh cong", "tp.hcm")
        String lower = VietnameseText.fold(VietnameseText.normalize(text)).toLowerCase(Locale.ROOT).trim();
        if (!ROAD.matcher(folded).find()) {
            Matcher abbreviated = ABBREVIATED_PREFIX.matcher(lower);
            if (abbreviated.find()) {
                String prefix = abbreviated.group(1) != null ? abbreviated.group(1) : abbreviated.group(2);
                return new Segment(text, prefix, key(lower.substring(abbreviated.end())));
            }
            Matcher word = WORD_PREFIX.matcher(folded);
            if (word.find()) {
                return new Segment(text, word.group(1), folded.substring(word.end()).trim());
            }
        }
        return new Segment(text, "", folded);
    }

    private static boolean isAdministrative(
        Segment s,
        Set<String> provinceNames,
        Set<String> districtNames,
        Set<String> localWardNames,
        String newWard
    ) {
        if (COUNTRY.contains(s.name()) && s.prefix().isEmpty()) {
            return true;
        }
        if (!s.prefix().isEmpty()) {
            return true;
        }
        return (
            provinceNames.contains(s.name()) ||
            districtNames.contains(s.name()) ||
            localWardNames.contains(s.name()) ||
            s.name().equals(newWard)
        );
    }

    /**
     * Đoạn ở cấp quận/huyện: có tiền tố quận/huyện/thị xã; hoặc "Thành phố X" / tên không tiền tố mà X là tên huyện cũ
     * (thành phố thuộc tỉnh), trừ khi X là tên tỉnh và không còn đoạn tỉnh nào phía sau ("Thành phố Hà Nội" ở cuối).
     */
    private static boolean isDistrictLevel(List<Segment> tail, int i, Set<String> provinceNames, Set<String> districtNames) {
        Segment s = tail.get(i);
        if (DISTRICT_LEVEL.contains(s.prefix())) {
            return true;
        }
        if (!s.prefix().isEmpty() && !CITY.contains(s.prefix())) {
            return false;
        }
        if (!districtNames.contains(s.name())) {
            return false;
        }
        if (!provinceNames.contains(s.name())) {
            return true;
        }
        // Tên vừa là tỉnh vừa là thành phố thuộc tỉnh (Bắc Ninh): là cấp huyện nếu phía sau còn đoạn tỉnh
        for (int j = i + 1; j < tail.size(); j++) {
            if (provinceNames.contains(tail.get(j).name())) {
                return true;
            }
        }
        return false;
    }

    /** Khóa so tên: bỏ dấu, chữ thường, bỏ dấu câu, gộp khoảng trắng. "TP. Hồ Chí Minh" → "tp ho chi minh". */
    public static String key(String text) {
        String folded = VietnameseText.fold(VietnameseText.normalize(text == null ? "" : text)).toLowerCase(Locale.ROOT);
        return SPACES.matcher(NON_WORD.matcher(folded).replaceAll(" ")).replaceAll(" ").trim();
    }

    /** Tên đơn vị (bỏ tiền tố, đã key), dùng để dựng các tập tên. "Thành phố Hà Nội" → "ha noi". */
    public static String nameKey(String unitName) {
        return parse(unitName).name();
    }
}
