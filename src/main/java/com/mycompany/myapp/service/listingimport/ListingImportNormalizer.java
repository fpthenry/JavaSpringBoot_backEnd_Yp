package com.mycompany.myapp.service.listingimport;

import com.mycompany.myapp.repository.search.VietnameseText;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Chuẩn hóa dữ liệu nhập để so trùng — hàm thuần, không đọc DB.
 *
 * <ul>
 *   <li>Mã số thuế: dạng chuẩn {@code 0123456789} hoặc chi nhánh {@code 0123456789-001} (giống dữ liệu đang có:
 *       1,36 triệu mã 10 số, 116 nghìn mã chi nhánh). Excel hay làm mất số 0 đầu → bù lại.</li>
 *   <li>Điện thoại: chỉ chữ số, +84 → 0; so theo 8 số cuối (mã vùng cũ 04 → 024 vẫn khớp).</li>
 *   <li>Tên: bỏ dấu, viết thường, bỏ cụm chỉ loại hình (công ty, TNHH, cổ phần…) — "CÔNG TY TNHH ABC" và
 *       "ABC Co., Ltd" cùng khóa {@code [abc]}.</li>
 * </ul>
 */
public final class ListingImportNormalizer {

    private static final Pattern TAX_10 = Pattern.compile("\\d{10}");
    private static final Pattern TAX_BRANCH = Pattern.compile("\\d{10}-\\d{3}");
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");

    /** Cụm chỉ loại hình doanh nghiệp (đã bỏ dấu) — có ở hầu hết tên nên không giúp phân biệt. Cụm dài trước. */
    private static final Pattern LEGAL_FORMS = Pattern.compile(
        "\\b(" +
            String.join(
                "|",
                "cong ty trach nhiem huu han",
                "trach nhiem huu han",
                "mot thanh vien",
                "hai thanh vien tro len",
                "cong ty co phan",
                "co phan",
                "cong ty",
                "cty",
                "tnhh",
                "mtv",
                "cp",
                "chi nhanh",
                "van phong dai dien",
                "vpdd",
                "doanh nghiep tu nhan",
                "dntn",
                "hop tac xa",
                "htx",
                "tap doan",
                "tong cong ty",
                "joint stock company",
                "company limited",
                "co ltd",
                "limited",
                "ltd",
                "jsc",
                "corporation",
                "corp",
                "company",
                "group"
            ) +
            ")\\b"
    );

    /** Từ chung của địa chỉ, bỏ khi so địa chỉ. */
    private static final Set<String> ADDRESS_STOP = Set.of(
        "so",
        "duong",
        "pho",
        "ngo",
        "ngach",
        "hem",
        "kiet",
        "to",
        "ap",
        "thon",
        "xom",
        "khu",
        "kp",
        "khom",
        "phuong",
        "p",
        "xa",
        "thi",
        "tran",
        "tt",
        "quan",
        "q",
        "huyen",
        "h",
        "tinh",
        "thanh",
        "tp",
        "viet",
        "nam",
        "vn"
    );

    private ListingImportNormalizer() {}

    /** Bỏ khoảng trắng thừa; rỗng → null. */
    public static String clean(String s) {
        if (s == null) {
            return null;
        }
        String t = s.replace(' ', ' ').trim().replaceAll("\\s+", " ");
        return t.isEmpty() ? null : t;
    }

    /**
     * Mã số thuế chuẩn, hoặc null nếu trống / sai định dạng (xem {@link #isInvalidTaxCode}).
     * {@code "0101 234 567"}, {@code "101234567"} (Excel mất số 0) → {@code 0101234567};
     * {@code "0101234567001"} → {@code 0101234567-001}.
     */
    public static String taxCode(String raw) {
        String s = clean(raw);
        if (s == null) {
            return null;
        }
        if (s.matches("\\d+\\.0+")) {
            s = s.substring(0, s.indexOf('.')); // ô số Excel đọc thành 101234567.0 — bỏ phần thập phân TRƯỚC khi bỏ dấu chấm
        }
        s = s.replaceAll("[\\s.]", "").replace('_', '-').replace('–', '-');
        if (s.matches("\\d{9}")) {
            s = "0" + s;
        } else if (s.matches("\\d{12}")) {
            s = "0" + s;
        } else if (s.matches("\\d{9}-\\d{3}")) {
            s = "0" + s;
        }
        if (s.matches("\\d{13}")) {
            s = s.substring(0, 10) + "-" + s.substring(10);
        }
        return TAX_10.matcher(s).matches() || TAX_BRANCH.matcher(s).matches() ? s : null;
    }

    /** Có nhập MST nhưng không đọc được. */
    public static boolean isInvalidTaxCode(String raw) {
        return clean(raw) != null && taxCode(raw) == null;
    }

    /**
     * Số điện thoại đầu tiên, chỉ còn chữ số; +84 / 84 → 0; mất số 0 đầu (Excel) thì bù. Dưới 8 chữ số → null.
     * {@code "(024) 3846.0171, 0912 345 678"} → {@code 02438460171}.
     */
    public static String phone(String raw) {
        String s = clean(raw);
        if (s == null) {
            return null;
        }
        String first = s.split("[,;/|]|\\s-\\s")[0];
        String digits = first.replaceAll("\\D", "");
        if (digits.startsWith("84") && digits.length() >= 11) {
            digits = "0" + digits.substring(2);
        } else if (digits.length() == 9 && !digits.startsWith("0")) {
            digits = "0" + digits;
        }
        return digits.length() >= 8 ? digits : null;
    }

    /** Hai số điện thoại (đã chuẩn hóa) là một: so 8 số cuối — đổi mã vùng (04 → 024) hay thiếu mã vùng vẫn khớp. */
    public static boolean samePhone(String a, String b) {
        if (a == null || b == null || a.length() < 8 || b.length() < 8) {
            return false;
        }
        return a.substring(a.length() - 8).equals(b.substring(b.length() - 8));
    }

    /** Chữ đã bỏ dấu, viết thường, chỉ còn chữ/số cách nhau một khoảng trắng. */
    public static String folded(String s) {
        String c = clean(s);
        if (c == null) {
            return "";
        }
        return NON_ALNUM.matcher(VietnameseText.fold(VietnameseText.normalize(c)).toLowerCase(Locale.ROOT))
            .replaceAll(" ")
            .trim();
    }

    /** Các từ phân biệt của tên (bỏ cụm loại hình). */
    public static Set<String> nameTokens(String name) {
        String f = LEGAL_FORMS.matcher(folded(name)).replaceAll(" ");
        return Arrays.stream(f.split(" "))
            .filter(w -> !w.isBlank())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Các từ phân biệt của tên, GIỮ DẤU, viết thường ("Công ty TNHH Tân Minh" → "tân minh"): để tìm ứng viên trên field
     * {@code name} của Elasticsearch (phân tích chuẩn, còn dấu) khi index chưa có {@code name.folded}.
     */
    public static String accentedNameWords(String name) {
        Set<String> tokens = nameTokens(name);
        String c = clean(name);
        if (c == null || tokens.isEmpty()) {
            return "";
        }
        return Arrays.stream(VietnameseText.normalize(c).toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
            .filter(w -> !w.isBlank() && tokens.contains(folded(w)))
            .distinct()
            .collect(Collectors.joining(" "));
    }

    /** Khóa tên để phát hiện trùng trong cùng đợt: các từ phân biệt nối lại. */
    public static String nameKey(String name) {
        return String.join(" ", nameTokens(name));
    }

    /** Các từ của địa chỉ, bỏ từ chung (số, đường, phường, quận…). */
    public static Set<String> addressTokens(String address) {
        return Arrays.stream(folded(address).split(" "))
            .filter(w -> !w.isBlank() && !ADDRESS_STOP.contains(w))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Hệ số Dice của hai tập từ: 1 = giống hệt, 0 = không chung từ nào. */
    public static double dice(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        long common = a.stream().filter(b::contains).count();
        return (2.0 * common) / (a.size() + b.size());
    }

    /**
     * Độ bao hàm: tỉ lệ từ của tập NHỎ hơn có trong tập kia (1 = tên ngắn nằm trọn trong tên dài).
     * Tập nhỏ dưới 2 từ → 0 (một từ như "An" có trong quá nhiều tên).
     */
    public static double containment(Set<String> a, Set<String> b) {
        Set<String> small = a.size() <= b.size() ? a : b;
        Set<String> large = small == a ? b : a;
        if (small.size() < 2) {
            return 0;
        }
        return (double) small.stream().filter(large::contains).count() / small.size();
    }

    /** Slug từ tên: {@code "Công ty TNHH Á Châu"} → {@code cong-ty-tnhh-a-chau} (tối đa 180 ký tự). */
    public static String slug(String name) {
        String s = folded(name).replace(' ', '-');
        if (s.length() > 180) {
            s = s.substring(0, 180).replaceAll("-[^-]*$", "");
        }
        return s.isEmpty() ? "doanh-nghiep" : s;
    }

    /** Cắt chuỗi cho vừa cột DB. */
    public static String cut(String s, int max) {
        String c = clean(s);
        return c == null || c.length() <= max ? c : c.substring(0, max);
    }

    // ---------------------------------------------------------------- tọa độ, URL ảnh

    /** Một số tọa độ: dấu trừ tùy chọn, phần nguyên, phần thập phân bằng dấu chấm. */
    private static final Pattern COORD_NUMBER = Pattern.compile("-?\\d{1,3}(\\.\\d+)?");
    private static final Pattern URL_IN_TEXT = Pattern.compile("https?://[^\\s\"'<>;|,\\]]+", Pattern.CASE_INSENSITIVE);

    /** Số ảnh tối đa mỗi doanh nghiệp. */
    public static final int MAX_IMAGES = 30;

    /** Kết quả đọc tọa độ: {@code value} dạng "vĩ độ:kinh độ" (như location_json đang lưu) hoặc null; {@code problem} để ghi chú. */
    public record Coordinate(String value, String problem) {}

    /**
     * Tọa độ từ ô "vĩ độ,kinh độ" / "vĩ độ:kinh độ" / "vĩ độ kinh độ", hoặc hai ô vĩ độ + kinh độ.
     * <b>Giữ nguyên chữ số</b> (không làm tròn, không đổi số chữ số thập phân) — chỉ đổi dấu phân cách thành {@code :}.
     * Ngoài vùng Việt Nam (vĩ độ 8–24, kinh độ 102–110, thường do nhập đảo hai số) → bỏ qua, có ghi chú.
     */
    public static Coordinate coordinate(String combined, String latitude, String longitude) {
        String lat;
        String lng;
        String c = clean(combined);
        if (c != null) {
            String[] parts = c.replaceAll("[()\\[\\]]", "").trim().split("\\s*[,:;]\\s*|\\s+");
            if (parts.length != 2) {
                return new Coordinate(null, "Tọa độ không đọc được: " + c);
            }
            lat = parts[0];
            lng = parts[1];
        } else {
            lat = clean(latitude);
            lng = clean(longitude);
            if (lat == null && lng == null) {
                return new Coordinate(null, null);
            }
            if (lat == null || lng == null) {
                return new Coordinate(null, "Thiếu vĩ độ hoặc kinh độ");
            }
        }
        if (!COORD_NUMBER.matcher(lat).matches() || !COORD_NUMBER.matcher(lng).matches()) {
            return new Coordinate(null, "Tọa độ không đọc được: " + lat + ", " + lng);
        }
        double la = Double.parseDouble(lat);
        double lo = Double.parseDouble(lng);
        if (la < 8 || la > 24 || lo < 102 || lo > 110) {
            return new Coordinate(null, "Tọa độ ngoài Việt Nam (có thể đảo vĩ độ/kinh độ), bỏ qua: " + lat + ", " + lng);
        }
        return new Coordinate(lat + ":" + lng, null);
    }

    /** URL http(s) hợp lệ (logo, ảnh); khác → null. */
    public static String httpUrl(String value) {
        String v = clean(value);
        if (v == null || !v.matches("(?i)https?://\\S+")) {
            return null;
        }
        return v;
    }

    /**
     * Danh sách URL ảnh: cách nhau bởi ; | , xuống dòng, hoặc mảng JSON (chuỗi hoặc đối tượng {"image_url": …}).
     * Chỉ http(s), bỏ trùng, tối đa {@link #MAX_IMAGES}.
     */
    public static List<String> imageUrls(String value) {
        List<String> out = new ArrayList<>();
        String v = clean(value);
        if (v == null) {
            return out;
        }
        Matcher m = URL_IN_TEXT.matcher(v.replace("\\/", "/"));
        Set<String> seen = new LinkedHashSet<>();
        while (m.find() && seen.size() < MAX_IMAGES) {
            seen.add(m.group());
        }
        out.addAll(seen);
        return out;
    }
}
