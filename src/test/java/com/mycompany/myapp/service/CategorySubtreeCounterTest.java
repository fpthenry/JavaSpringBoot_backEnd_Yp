package com.mycompany.myapp.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CategorySubtreeCounterTest {

    @Test
    void demCaNganhConMoiDoanhNghiepMotLanBoQuaBaiNhap() {
        // 1 Xây dựng > 2 Lắp đặt > 3 Lắp đặt điện, 4 Lắp đặt nước; 5 Nông nghiệp; 8 <-> 9 vòng lặp (dữ liệu lỗi)
        Map<Long, Long> parents = new HashMap<>();
        parents.put(1L, null);
        parents.put(2L, 1L);
        parents.put(3L, 2L);
        parents.put(4L, 2L);
        parents.put(5L, null);
        parents.put(8L, 9L);
        parents.put(9L, 8L);
        CategorySubtreeCounter counter = new CategorySubtreeCounter(parents, Set.of(103L));

        // Đọc theo thứ tự listing_id như khóa chính bảng nối
        counter.accept(100, 3); // listing 100 gắn cả điện và nước: Lắp đặt, Xây dựng chỉ đếm 1 lần
        counter.accept(100, 4);
        counter.accept(101, 3);
        counter.accept(102, 5);
        counter.accept(103, 3); // bài nháp: bỏ qua
        counter.accept(104, 8);
        counter.accept(105, 999); // ngành không tồn tại: bỏ qua

        Map<Long, Long> result = counter.result();
        assertThat(result).containsEntry(3L, 2L).containsEntry(4L, 1L).containsEntry(2L, 2L).containsEntry(1L, 2L);
        assertThat(result).containsEntry(5L, 1L).containsEntry(8L, 1L).containsEntry(9L, 1L);
        assertThat(result).doesNotContainKey(999L).hasSize(7);
    }
}
