package com.mycompany.myapp.service.dto.publicapi;

import java.io.Serializable;

/**
 * Một đơn vị hành chính trong mục lục tỉnh thành.
 *
 * @param type         province, district (chỉ bộ cũ), ward.
 * @param isNew        thuộc địa giới mới (từ 01/7/2025).
 * @param letter       chữ cái đầu của tên riêng (bỏ "Xã", "Phường", "Tỉnh"...), bỏ dấu nhưng giữ Đ; "#" nếu không phải chữ cái.
 * @param parentName   tên đơn vị cha trực tiếp (null với tỉnh).
 * @param provinceName tên tỉnh chứa đơn vị (chính nó nếu là tỉnh), để phân biệt các xã trùng tên.
 * @param listingCount số doanh nghiệp đã xuất bản của đơn vị, tính cả các đơn vị con.
 */
public record PublicLocationDTO(
    Long id,
    String name,
    String slug,
    String type,
    boolean isNew,
    String letter,
    Long parentId,
    String parentName,
    String provinceName,
    long listingCount
) implements Serializable {
    /** Một chữ cái trong mục lục và số đơn vị bắt đầu bằng chữ đó. */
    public record Letter(String letter, long count) implements Serializable {}
}
