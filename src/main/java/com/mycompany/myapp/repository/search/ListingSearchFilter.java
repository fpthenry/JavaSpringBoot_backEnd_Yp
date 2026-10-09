package com.mycompany.myapp.repository.search;

import org.springframework.util.StringUtils;

/**
 * Điều kiện tìm doanh nghiệp trên Elasticsearch. Mọi field đều không bắt buộc.
 *
 * @param query          từ khóa (cú pháp query_string; phải có đủ mọi chữ). Tìm trên mọi field, gồm địa chỉ cũ và mới.
 * @param locationTreeId địa phương (bộ cũ hoặc mới); lấy cả các đơn vị con.
 * @param categoryTreeId ngành nghề; lấy cả các ngành con.
 */
public record ListingSearchFilter(String query, Long locationTreeId, Long categoryTreeId) {
    public boolean hasQuery() {
        return StringUtils.hasText(query);
    }
}
