package com.mycompany.myapp.service.dto.publicapi;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * Doanh nghiệp trả cho FE qua API công khai (chỉ doanh nghiệp đã xuất bản).
 *
 * @param wpId        id bài WordPress cũ, để FE chuyển hướng URL cũ dạng {@code ?p=}.
 * @param description giới thiệu (chữ thuần, có xuống dòng); chỉ có ở API chi tiết, danh sách trả null.
 * @param coordinate  tọa độ nguyên văn như trong dữ liệu ("vĩ độ:kinh độ"), không làm tròn; null nếu không có.
 * @param thumbnail   URL logo/ảnh đại diện; null nếu chưa có URL (dữ liệu cũ chỉ có id ảnh WordPress).
 * @param images      URL ảnh thư viện (chỉ ở API chi tiết, danh sách trả rỗng).
 * @param categories  ngành nghề gắn trực tiếp, sắp theo tên.
 * @param locations   địa phương gắn trực tiếp (bộ cũ và mới), tỉnh trước rồi đến cấp dưới.
 */
public record PublicListingDTO(
    Long id,
    Long wpId,
    String name,
    String nameEn,
    String nameAlias,
    String slug,
    String description,
    String phone,
    String mobile,
    String email,
    String website,
    String address,
    String addressNew,
    String coordinate,
    String taxCode,
    String representative,
    String capital,
    String foundedYear,
    String businessType,
    String businessStatus,
    String industryCode,
    String managedBy,
    String thumbnail,
    List<String> images,
    boolean featured,
    Integer viewCount,
    Instant publishedAt,
    Instant modifiedAt,
    List<Term> categories,
    List<Term> locations
) implements Serializable {
    /**
     * Ngành nghề hoặc địa phương gắn với doanh nghiệp.
     *
     * @param type với địa phương: province, district, ward; với ngành nghề: null.
     */
    public record Term(Long id, String name, String slug, Long parentId, String type) implements Serializable {}

    /** Một gợi ý khi gõ ô tìm kiếm. */
    public record Suggestion(Long id, String name, String slug) implements Serializable {}
}
