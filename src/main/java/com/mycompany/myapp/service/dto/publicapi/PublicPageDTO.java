package com.mycompany.myapp.service.dto.publicapi;

import java.io.Serializable;
import java.util.List;

/**
 * Một trang kết quả của API công khai: FE đọc tổng số ngay trong body, không cần đọc header.
 *
 * @param page       số trang, bắt đầu từ 0.
 * @param totalItems tổng số kết quả.
 */
public record PublicPageDTO<T>(List<T> items, int page, int size, long totalItems, int totalPages) implements Serializable {}
