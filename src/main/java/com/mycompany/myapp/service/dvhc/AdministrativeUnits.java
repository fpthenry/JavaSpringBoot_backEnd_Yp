package com.mycompany.myapp.service.dvhc;

import com.mycompany.myapp.repository.search.VietnameseText;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Quy ước đơn vị hành chính trong bảng {@code location}.
 * <ul>
 *   <li><b>Bộ cũ</b> (trước 01/7/2025): tỉnh → huyện → xã, 3 cấp. Slug kết thúc bằng mã: {@code huyen-ba-vi-271}.</li>
 *   <li><b>Bộ mới</b> (từ 01/7/2025): tỉnh → xã, 2 cấp. Slug kết thúc bằng mã và {@value #NEW_SUFFIX}:
 *   {@code phuong-cua-nam-00082-2025}. Đây là dấu hiệu duy nhất phân biệt hai bộ (mã trùng nhau: Hà Nội là 01 ở cả hai).</li>
 * </ul>
 * Mã theo danh mục của Tổng cục Thống kê: tỉnh 2 chữ số, huyện 3, xã 5. Cột {@code code} lưu mã này.
 */
public final class AdministrativeUnits {

    public static final String NEW_SUFFIX = "-2025";
    public static final String PROVINCE = "province";
    public static final String DISTRICT = "district";
    public static final String WARD = "ward";

    private static final Pattern CODE_IN_SLUG = Pattern.compile("-(\\d+)(?:" + NEW_SUFFIX + ")?$");
    private static final Pattern NON_SLUG = Pattern.compile("[^a-z0-9]+");
    private static final Pattern DASHES = Pattern.compile("\\s*[-–—]\\s*");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private AdministrativeUnits() {}

    public static boolean isNew(String slug) {
        return slug != null && slug.endsWith(NEW_SUFFIX);
    }

    /** Mã ở cuối slug ({@code huyen-ba-vi-271} → 271, {@code phuong-ba-dinh-00004-2025} → 00004); không có thì null. */
    public static String codeFromSlug(String slug) {
        if (slug == null) {
            return null;
        }
        Matcher matcher = CODE_IN_SLUG.matcher(slug);
        return matcher.find() ? matcher.group(1) : null;
    }

    /** Slug của đơn vị bộ mới: tên bỏ dấu + mã + {@value #NEW_SUFFIX}. */
    public static String newSlug(String name, String code) {
        return slugify(name) + "-" + code + NEW_SUFFIX;
    }

    /** "Phường Cửa Nam" → "phuong-cua-nam"; "Xã Chân Mây – Lăng Cô" → "xa-chan-may-lang-co". */
    public static String slugify(String name) {
        String folded = VietnameseText.fold(VietnameseText.normalize(name == null ? "" : name)).toLowerCase(Locale.ROOT);
        String slug = NON_SLUG.matcher(folded).replaceAll("-");
        return slug.replaceAll("^-+|-+$", "");
    }

    /**
     * Khóa so khớp tên: chuẩn hóa tiếng Việt (Unicode dựng sẵn, kiểu đặt dấu mới: Thuỷ = Thủy), chữ thường, gạch ngang,
     * dấu nháy (’ = ') và khoảng trắng thống nhất. Vẫn giữ dấu, nên "Bàn" khác "Bán".
     */
    public static String nameKey(String name) {
        String normalized = VietnameseText.normalize(name == null ? "" : name)
            .toLowerCase(Locale.ROOT)
            .replace('’', '\'')
            .replace('‘', '\'')
            .trim();
        return SPACES.matcher(DASHES.matcher(normalized).replaceAll(" - ")).replaceAll(" ");
    }
}
