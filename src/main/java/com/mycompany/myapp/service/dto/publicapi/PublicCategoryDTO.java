package com.mycompany.myapp.service.dto.publicapi;

import java.io.Serializable;

/**
 * Một ngành nghề trả cho FE qua API công khai.
 *
 * @param code         mã ngành VSIC 2025 (A–V ở cấp 1, 2–5 chữ số ở cấp 2–5); null với ngành chưa có mã.
 * @param level        cấp trong hệ thống ngành (1–5).
 * @param letter       chữ cái đầu dùng để nhóm (bỏ dấu: Ô → O, Đ → D); "#" nếu không bắt đầu bằng chữ cái.
 * @param parentName   tên ngành cha, để phân biệt các ngành trùng tên ở cấp khác nhau (có thể null).
 * @param listingCount số doanh nghiệp đã xuất bản, tính cả ngành con.
 */
public record PublicCategoryDTO(
    Long id,
    String code,
    String name,
    String slug,
    String letter,
    Integer level,
    Long parentId,
    String parentName,
    long listingCount
) implements Serializable {
    /**
     * Một chữ cái trong mục lục.
     *
     * @param count số ngành bắt đầu bằng chữ này.
     */
    public record Letter(String letter, long count) implements Serializable {}

    /**
     * Slug ngành cũ → slug mới.
     *
     * @param targetSlug slug ngành mới; null: ngành cũ không có ngành tương ứng, chuyển về mục lục ngành nghề.
     */
    public record Redirect(String oldSlug, String targetSlug) implements Serializable {}
}
