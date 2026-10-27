package com.mycompany.myapp.service.listingimport;

import com.mycompany.myapp.repository.search.VietnameseText;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Một doanh nghiệp đọc từ nguồn ngoài (dòng file Excel/CSV, phần tử JSON của API), chưa chuẩn hóa.
 * Tên field cũng là tên cột chuẩn của file mẫu; cột / khóa JSON khác tên được nhận qua {@link #ALIASES}.
 *
 * @param categories ngành nghề: mã VSIC (4690), slug hoặc tên, cách nhau bởi ; | , hoặc xuống dòng
 * @param province   tỉnh/thành: tên ("Hà Nội", "TP Hồ Chí Minh"), mã (01, 79) hoặc slug
 * @param district   quận/huyện (địa giới cũ, không bắt buộc): giúp phân biệt xã cũ trùng tên trong cùng tỉnh
 * @param ward       phường/xã: tên ("Phường Ba Đình", "Ba Đình") hoặc mã (00004); tìm trong tỉnh của dòng
 * @param coordinate tọa độ "vĩ độ,kinh độ" hoặc "vĩ độ:kinh độ" — giữ nguyên chữ số; hoặc dùng latitude + longitude
 * @param logo       URL logo (http/https) → listing.thumbnail
 * @param images     URL ảnh cách nhau bởi ; | xuống dòng, hoặc mảng JSON → listing.images
 */
public record ImportRecord(
    String sourceRef,
    String name,
    String nameEn,
    String nameAlias,
    String taxCode,
    String phone,
    String mobile,
    String email,
    String website,
    String address,
    String addressNew,
    String province,
    String district,
    String ward,
    String coordinate,
    String latitude,
    String longitude,
    String representative,
    String capital,
    String foundedYear,
    String businessType,
    String businessStatus,
    String industryCode,
    String categories,
    String managedBy,
    String description,
    String logo,
    String images
) {
    /** Tên cột chuẩn theo thứ tự của file mẫu (tọa độ: dùng coordinate HOẶC latitude + longitude). */
    public static final String[] FIELDS = {
        "sourceRef",
        "name",
        "nameEn",
        "nameAlias",
        "taxCode",
        "phone",
        "mobile",
        "email",
        "website",
        "address",
        "addressNew",
        "province",
        "district",
        "ward",
        "coordinate",
        "latitude",
        "longitude",
        "representative",
        "capital",
        "foundedYear",
        "businessType",
        "businessStatus",
        "industryCode",
        "categories",
        "managedBy",
        "description",
        "logo",
        "images",
    };

    /** Tên cột (đã bỏ dấu, viết thường, bỏ ký tự ngoài chữ/số) → field. */
    private static final Map<String, String> ALIASES = new HashMap<>();

    static {
        alias("sourceRef", "sourceref", "id", "ma", "madn", "madoanhnghiep", "sourceid", "ref", "externalid");
        alias("name", "name", "ten", "tencongty", "tendoanhnghiep", "tencty", "companyname", "tendaydu");
        alias("nameEn", "nameen", "tentienganh", "tenquocte", "tennuocngoai", "englishname", "internationalname");
        alias("nameAlias", "namealias", "tenviettat", "viettat", "alias", "shortname");
        alias("taxCode", "taxcode", "mst", "masothue", "masodoanhnghiep", "msdn", "tax", "taxid");
        alias("phone", "phone", "dienthoai", "sodienthoai", "sdt", "dt", "tel", "telephone", "phonenumber");
        alias("mobile", "mobile", "didong", "sodidong", "hotline", "mobilephone");
        alias("email", "email", "mail", "thudientu");
        alias("website", "website", "web", "trangweb", "url");
        alias("address", "address", "diachi", "diachitrusochinh", "trusochinh", "diachitruso", "diachicu");
        alias("addressNew", "addressnew", "newaddress", "diachimoi", "diachisausapnhap", "diachimoisausapnhap", "diachitheodiagioimoi");
        alias("province", "province", "tinhthanh", "tinh", "thanhpho", "tinhthanhpho", "city");
        alias("district", "district", "quanhuyen", "quan", "huyen", "thixa");
        alias("ward", "ward", "phuongxa", "xaphuong", "phuong", "xa", "thitran", "commune");
        alias("coordinate", "coordinate", "coordinates", "toado", "vitri", "location", "latlng", "latlong", "gps");
        alias("latitude", "latitude", "lat", "vido");
        alias("longitude", "longitude", "lng", "lon", "long", "kinhdo");
        alias("representative", "representative", "nguoidaidien", "daidien", "nguoidaidienphapluat", "giamdoc", "director");
        alias("capital", "capital", "vondieule", "von");
        alias("foundedYear", "foundedyear", "ngaythanhlap", "namthanhlap", "ngaycap", "ngayhoatdong", "founded", "founddate");
        alias("businessType", "businesstype", "loaihinh", "loaihinhdoanhnghiep", "loaihinhdn", "type");
        alias("businessStatus", "businessstatus", "tinhtrang", "trangthai", "tinhtranghoatdong", "status");
        alias("industryCode", "industrycode", "manganh", "manganhchinh", "nganhnghechinh", "nganhchinh", "vsic");
        alias("categories", "categories", "category", "nganhnghe", "nganh", "nganhnghekinhdoanh", "linhvuc");
        alias("managedBy", "managedby", "coquanthue", "coquanthuequanly", "coquanquanly", "noiquanly", "quanlyboi", "quanlythue");
        alias("description", "description", "mota", "gioithieu", "noidung");
        alias("logo", "logo", "logourl", "anhdaidien", "hinhdaidien", "thumbnail", "avatar");
        alias("images", "images", "image", "hinhanh", "anh", "thuvienanh", "gallery", "photos", "imageurls");
    }

    private static void alias(String field, String... names) {
        for (String n : names) {
            ALIASES.put(key(n), field);
        }
    }

    private static String key(String header) {
        return VietnameseText.fold(header).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /** Tên cột / khóa JSON → field chuẩn; không nhận ra thì null (cột bị bỏ qua). */
    public static String fieldOf(String header) {
        return header == null ? null : ALIASES.get(key(header));
    }

    /** Dựng từ các cặp field chuẩn → giá trị (giá trị rỗng coi như không có). */
    public static ImportRecord of(Map<String, String> v) {
        return new ImportRecord(
            v.get("sourceRef"),
            v.get("name"),
            v.get("nameEn"),
            v.get("nameAlias"),
            v.get("taxCode"),
            v.get("phone"),
            v.get("mobile"),
            v.get("email"),
            v.get("website"),
            v.get("address"),
            v.get("addressNew"),
            v.get("province"),
            v.get("district"),
            v.get("ward"),
            v.get("coordinate"),
            v.get("latitude"),
            v.get("longitude"),
            v.get("representative"),
            v.get("capital"),
            v.get("foundedYear"),
            v.get("businessType"),
            v.get("businessStatus"),
            v.get("industryCode"),
            v.get("categories"),
            v.get("managedBy"),
            v.get("description"),
            v.get("logo"),
            v.get("images")
        );
    }

    /** Ngược với {@link #of}: lưu vào listing_import_row.data để xử lý lại khi duyệt. */
    public Map<String, String> toMap() {
        Map<String, String> m = new LinkedHashMap<>();
        put(m, "sourceRef", sourceRef);
        put(m, "name", name);
        put(m, "nameEn", nameEn);
        put(m, "nameAlias", nameAlias);
        put(m, "taxCode", taxCode);
        put(m, "phone", phone);
        put(m, "mobile", mobile);
        put(m, "email", email);
        put(m, "website", website);
        put(m, "address", address);
        put(m, "addressNew", addressNew);
        put(m, "province", province);
        put(m, "district", district);
        put(m, "ward", ward);
        put(m, "coordinate", coordinate);
        put(m, "latitude", latitude);
        put(m, "longitude", longitude);
        put(m, "representative", representative);
        put(m, "capital", capital);
        put(m, "foundedYear", foundedYear);
        put(m, "businessType", businessType);
        put(m, "businessStatus", businessStatus);
        put(m, "industryCode", industryCode);
        put(m, "categories", categories);
        put(m, "managedBy", managedBy);
        put(m, "description", description);
        put(m, "logo", logo);
        put(m, "images", images);
        return m;
    }

    private static void put(Map<String, String> m, String k, String v) {
        if (v != null && !v.isBlank()) {
            m.put(k, v);
        }
    }
}
