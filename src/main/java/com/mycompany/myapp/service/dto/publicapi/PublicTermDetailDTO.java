package com.mycompany.myapp.service.dto.publicapi;

import java.io.Serializable;
import java.util.List;

/**
 * Chi tiết một ngành nghề hoặc một địa phương: dùng cho trang /nganh-nghe/{slug}, /tinh-thanh/{slug} (tiêu đề, breadcrumb,
 * danh sách con).
 *
 * @param slug         slug hiện tại. Với địa phương, nếu FE hỏi bằng slug cũ của website WordPress (đã đổi khi chuẩn hóa
 *                     địa giới 2025) thì slug trả về khác slug hỏi: FE chuyển hướng 301 sang slug này.
 * @param description  mô tả (chỉ ngành nghề có; địa phương null).
 * @param type         địa phương: province, district, ward; ngành nghề: null.
 * @param isNew        địa phương thuộc địa giới mới (từ 01/7/2025); ngành nghề: null.
 * @param listingCount số doanh nghiệp đã xuất bản, tính cả cấp con.
 * @param ancestors    các cấp cha, từ gốc xuống cha trực tiếp (breadcrumb).
 * @param children     các cấp con trực tiếp: ngành nghề sắp theo mã ngành, địa phương sắp theo tên.
 * @param code         ngành nghề: mã ngành VSIC 2025 (A–V, 2–5 chữ số); địa phương: null.
 * @param level        ngành nghề: cấp 1–5; địa phương: null.
 */
public record PublicTermDetailDTO(
    Long id,
    String name,
    String slug,
    String description,
    String type,
    Boolean isNew,
    long listingCount,
    List<Ref> ancestors,
    List<Ref> children,
    String code,
    Integer level
) implements Serializable {
    /** Địa phương (không có mã ngành, cấp). */
    public PublicTermDetailDTO(
        Long id,
        String name,
        String slug,
        String description,
        String type,
        Boolean isNew,
        long listingCount,
        List<Ref> ancestors,
        List<Ref> children
    ) {
        this(id, name, slug, description, type, isNew, listingCount, ancestors, children, null, null);
    }

    /** Một cấp cha hoặc con; code, level chỉ ngành nghề có. */
    public record Ref(Long id, String name, String slug, long listingCount, String code, Integer level) implements Serializable {
        public Ref(Long id, String name, String slug, long listingCount) {
            this(id, name, slug, listingCount, null, null);
        }
    }
}
