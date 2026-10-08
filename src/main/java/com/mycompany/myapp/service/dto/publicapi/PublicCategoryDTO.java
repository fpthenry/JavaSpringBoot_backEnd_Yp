package com.mycompany.myapp.service.dto.publicapi;

import java.io.Serializable;

/**
 * Một ngành nghề trả cho FE qua API công khai.
 *
 * @param letter       chữ cái đầu dùng để nhóm (bỏ dấu: Ô → O, Đ → D); "#" nếu không bắt đầu bằng chữ cái.
 * @param parentName   tên ngành cha, để phân biệt các ngành trùng tên ở cấp khác nhau (có thể null).
 * @param listingCount số doanh nghiệp đã xuất bản, tính cả ngành con.
 */
public record PublicCategoryDTO(
    Long id,
    String name,
    String slug,
    String letter,
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
}
