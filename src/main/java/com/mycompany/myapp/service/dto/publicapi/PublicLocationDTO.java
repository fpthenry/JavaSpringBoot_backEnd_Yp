package com.mycompany.myapp.service.dto.publicapi;

import java.io.Serializable;
import java.util.List;

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

    /**
     * Đơn vị cũ (trước 01/7/2025) khớp từ khóa, kèm các đơn vị mới hiện chứa nó.
     *
     * @param type         ward, district hoặc province (bộ cũ).
     * @param parentName   quận/huyện cũ của xã (null với huyện, tỉnh).
     * @param provinceName tỉnh cũ (null với tỉnh).
     * @param targets      đơn vị bộ mới: xã cũ → 1 xã mới, hoặc nhiều nếu bị chia; huyện cũ → các xã mới; tỉnh cũ → tỉnh mới.
     */
    public record Former(
        String name,
        String type,
        String parentName,
        String provinceName,
        List<PublicLocationDTO> targets
    ) implements Serializable {}

    /**
     * Một đơn vị cũ đã nhập vào đơn vị mới (theo bảng chuyển đổi).
     *
     * @param id      id đơn vị cũ trong bảng location (null nếu không có, vd xã cũ chưa từng có trên website).
     * @param code    mã đơn vị cũ (tỉnh 2 số, huyện 3 số, xã 5 số).
     * @param partial chỉ một phần đơn vị cũ nhập vào (phần còn lại sang đơn vị khác).
     */
    public record Origin(
        Long id,
        String code,
        String name,
        String type,
        String parentName,
        String provinceName,
        boolean partial
    ) implements Serializable {}

    /**
     * Một nút trong cây tra cứu của trang quản trị (gọn hơn bản đầy đủ: cây bộ cũ ~12.000 đơn vị).
     *
     * @param code   mã ĐVHC ở cuối slug (null nếu slug không có mã).
     * @param mapped có trong bảng chuyển đổi: đơn vị cũ thì biết nay thuộc đâu, đơn vị mới thì biết hình thành từ đâu.
     */
    public record Node(
        long id,
        String code,
        String name,
        String type,
        Long parentId,
        long listingCount,
        boolean mapped
    ) implements Serializable {}
}
