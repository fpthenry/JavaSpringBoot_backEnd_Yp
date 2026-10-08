package com.mycompany.myapp.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Đếm số doanh nghiệp của mỗi ngành, tính cả các ngành con, mỗi doanh nghiệp đếm một lần.
 * <p>
 * Đọc các cặp (listing, ngành) <b>theo thứ tự listing_id</b> (đúng thứ tự khóa chính bảng nối, đọc tuần tự rất nhanh).
 * Với mỗi listing, cộng 1 cho ngành của nó và mọi ngành tổ tiên; mảng {@code lastListing} đảm bảo một listing gắn
 * nhiều ngành cùng nhánh chỉ được cộng một lần cho mỗi tổ tiên. Bộ nhớ chỉ cỡ số ngành, không phụ thuộc số dòng.
 */
public class CategorySubtreeCounter {

    private final Map<Long, Integer> indexById = new HashMap<>();
    private final long[] ids;
    /** Tổ tiên của mỗi ngành (gồm chính nó), theo chỉ số mảng. */
    private final int[][] ancestors;
    private final long[] counts;
    private final long[] lastListing;
    private final Set<Long> excludedListings;

    /**
     * @param parentById       id ngành → id ngành cha (null với ngành gốc).
     * @param excludedListings listing không đếm (bài nháp).
     */
    public CategorySubtreeCounter(Map<Long, Long> parentById, Set<Long> excludedListings) {
        this.ids = parentById.keySet().stream().mapToLong(Long::longValue).toArray();
        for (int i = 0; i < ids.length; i++) {
            indexById.put(ids[i], i);
        }
        this.ancestors = new int[ids.length][];
        for (int i = 0; i < ids.length; i++) {
            List<Integer> chain = new ArrayList<>();
            Set<Long> seen = new HashSet<>();
            Long id = ids[i];
            // Đi ngược lên gốc; dừng khi gặp lại (cây lỗi có vòng lặp) hoặc cha không tồn tại
            while (id != null && seen.add(id) && indexById.containsKey(id)) {
                chain.add(indexById.get(id));
                id = parentById.get(id);
            }
            ancestors[i] = chain.stream().mapToInt(Integer::intValue).toArray();
        }
        this.counts = new long[ids.length];
        this.lastListing = new long[ids.length];
        java.util.Arrays.fill(lastListing, Long.MIN_VALUE);
        this.excludedListings = excludedListings;
    }

    /** Một cặp (listing, ngành). Ngành không có trong danh sách thì bỏ qua. */
    public void accept(long listingId, long categoryId) {
        if (excludedListings.contains(listingId)) {
            return;
        }
        Integer index = indexById.get(categoryId);
        if (index == null) {
            return;
        }
        for (int ancestor : ancestors[index]) {
            if (lastListing[ancestor] != listingId) {
                lastListing[ancestor] = listingId;
                counts[ancestor]++;
            }
        }
    }

    /** id ngành → số doanh nghiệp (gồm ngành con). Ngành không có doanh nghiệp nào trả 0. */
    public Map<Long, Long> result() {
        Map<Long, Long> result = new HashMap<>();
        for (int i = 0; i < ids.length; i++) {
            result.put(ids[i], counts[i]);
        }
        return result;
    }
}
