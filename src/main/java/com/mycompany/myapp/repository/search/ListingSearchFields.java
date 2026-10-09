package com.mycompany.myapp.repository.search;

import com.mycompany.myapp.domain.Category;
import com.mycompany.myapp.domain.Listing;
import com.mycompany.myapp.domain.Location;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Điền các field chỉ có trong Elasticsearch của {@link Listing}: {@code categoryIds}, {@code locationIds} (gồm id mọi cấp
 * cha, để lọc theo cả cây) và {@code sortId}.
 * <ul>
 *   <li>{@link #fill(Listing)}: lúc lưu doanh nghiệp, trong transaction đang lưu (dùng quan hệ trên entity, có thể chỉ có id).</li>
 *   <li>{@link #fillFromDatabase(List)}: lúc reindex, đọc liên kết của cả lô bằng 2 câu JDBC (không lazy-load từng doanh nghiệp).</li>
 * </ul>
 * Cây cha của ngành (~2.400) và địa phương (~15.300) giữ trong bộ nhớ, đọc lại sau {@link #PARENTS_TTL}: đổi cây ngành
 * hoặc địa phương thì phải reindex listing để các doanh nghiệp đã index nhận cây mới.
 */
@Component
public class ListingSearchFields {

    static final Duration PARENTS_TTL = Duration.ofMinutes(5);

    /** id → id cha (null với gốc) của ngành và địa phương. */
    record Parents(Map<Long, Long> category, Map<Long, Long> location) {}

    private final JdbcTemplate jdbcTemplate;
    private volatile Parents parents;
    private volatile Instant parentsLoadedAt = Instant.EPOCH;

    public ListingSearchFields(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /** Lúc lưu: lấy id ngành, địa phương từ quan hệ trên entity. */
    public Listing fill(Listing listing) {
        Parents p = parents();
        listing.setCategoryIds(withAncestors(ids(listing.getCategories(), Category::getId), p.category()));
        listing.setLocationIds(withAncestors(ids(listing.getLocations(), Location::getId), p.location()));
        listing.setSortId(listing.getId());
        return listing;
    }

    /** Lúc reindex: đọc liên kết của cả lô từ bảng nối. */
    public List<Listing> fillFromDatabase(List<Listing> listings) {
        if (listings.isEmpty()) {
            return listings;
        }
        Parents p = parents();
        String in = listings
            .stream()
            .map(l -> String.valueOf(l.getId()))
            .collect(Collectors.joining(","));
        Map<Long, Set<Long>> categories = links(
            "select listing_id, category_id from rel_listing__category where listing_id in (" + in + ")"
        );
        Map<Long, Set<Long>> locations = links(
            "select listing_id, location_id from rel_listing__location where listing_id in (" + in + ")"
        );
        for (Listing listing : listings) {
            listing.setCategoryIds(withAncestors(categories.getOrDefault(listing.getId(), Set.of()), p.category()));
            listing.setLocationIds(withAncestors(locations.getOrDefault(listing.getId(), Set.of()), p.location()));
            listing.setSortId(listing.getId());
        }
        return listings;
    }

    /** Các id và mọi cấp cha của chúng; cây lỗi có vòng lặp cũng không treo. */
    static Set<Long> withAncestors(Collection<Long> ids, Map<Long, Long> parents) {
        Set<Long> result = new HashSet<>();
        for (Long id : ids) {
            Long current = id;
            while (current != null && result.add(current)) {
                current = parents.get(current);
            }
        }
        return result;
    }

    private static <T> Set<Long> ids(Set<T> items, java.util.function.Function<T, Long> id) {
        return items == null ? Set.of() : items.stream().map(id).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private Map<Long, Set<Long>> links(String sql) {
        Map<Long, Set<Long>> result = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            result.computeIfAbsent(rs.getLong(1), k -> new HashSet<>()).add(rs.getLong(2));
        });
        return result;
    }

    Parents parents() {
        Parents current = parents;
        if (current == null || Instant.now().isAfter(parentsLoadedAt.plus(PARENTS_TTL))) {
            current = new Parents(parentMap("select id, parent_id from category"), parentMap("select id, parent_id from location"));
            parents = current;
            parentsLoadedAt = Instant.now();
        }
        return current;
    }

    private Map<Long, Long> parentMap(String sql) {
        Map<Long, Long> result = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            result.put(rs.getLong(1), rs.getObject(2, Long.class));
        });
        return result;
    }
}
