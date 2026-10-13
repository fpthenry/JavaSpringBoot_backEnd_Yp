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
 * @param children     các cấp con trực tiếp, sắp theo tên.
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
    List<Ref> children
) implements Serializable {
    /** Một cấp cha hoặc con. */
    public record Ref(Long id, String name, String slug, long listingCount) implements Serializable {}
}
