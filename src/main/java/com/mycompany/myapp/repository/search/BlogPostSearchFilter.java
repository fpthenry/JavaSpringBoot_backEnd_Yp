package com.mycompany.myapp.repository.search;

/**
 * Điều kiện tìm bài viết trên Elasticsearch. Mọi field đều không bắt buộc.
 *
 * @param query      từ khóa, có dấu hay không dấu đều được; tìm trong tiêu đề, tóm tắt, nội dung, tên danh mục, tên thẻ.
 * @param categoryId chỉ lấy bài thuộc danh mục này hoặc danh mục con của nó.
 * @param tagId      chỉ lấy bài có thẻ này.
 * @param status     {@code publish} hoặc {@code draft}.
 */
public record BlogPostSearchFilter(String query, Long categoryId, Long tagId, String status) {
    public static BlogPostSearchFilter ofQuery(String query) {
        return new BlogPostSearchFilter(query, null, null, null);
    }
}
