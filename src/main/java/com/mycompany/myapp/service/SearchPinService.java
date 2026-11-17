package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.SearchPin;
import com.mycompany.myapp.repository.SearchPinRepository;
import com.mycompany.myapp.repository.search.VietnameseText;
import com.mycompany.myapp.security.SecurityUtils;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Viết tay (2026-11-17): ghim doanh nghiệp lên đầu kết quả tìm kiếm theo từ khóa (thay "Custom Search Results" của
 * ElasticPress). Khách tìm đúng từ khóa (không phân biệt dấu, hoa thường, dấu câu) thì các doanh nghiệp ghim hiện đầu tiên,
 * theo thứ tự đã xếp, sau đó mới đến kết quả bình thường. Bảng nhỏ nên giữ sẵn trong bộ nhớ, đổi gì thì nạp lại.
 */
@Service
@Transactional
public class SearchPinService {

    /** Tối đa số doanh nghiệp ghim cho một từ khóa (một trang kết quả). */
    public static final int MAX_PINS_PER_KEYWORD = 20;

    private static final Logger LOG = LoggerFactory.getLogger(SearchPinService.class);

    private final SearchPinRepository repository;
    private final JdbcTemplate jdbcTemplate;

    /** Khóa so khớp → id doanh nghiệp theo thứ tự ghim. null = chưa nạp. */
    private volatile Map<String, List<Long>> cache;

    public SearchPinService(SearchPinRepository repository, JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Một doanh nghiệp đang được ghim. */
    public record PinItem(
        Long id,
        Long listingId,
        String name,
        String slug,
        String taxCode,
        String status,
        int position
    ) implements Serializable {}

    /** Một từ khóa và các doanh nghiệp ghim của nó, theo thứ tự. */
    public record PinGroup(String keyword, String key, List<PinItem> items) implements Serializable {}

    /** Thêm một doanh nghiệp vào từ khóa. */
    public record NewPin(String keyword, Long listingId) {}

    /** Xếp lại thứ tự trong một từ khóa: danh sách id ghim theo thứ tự mới. */
    public record Reorder(List<Long> pinIds) {}

    /**
     * Khóa so khớp: bỏ dấu, chữ thường, chỉ giữ chữ và số, cách nhau một khoảng trắng.
     * "Máy nén khí", "may nen khi", "MÁY NÉN-KHÍ" cùng ra "may nen khi".
     */
    public static String key(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String folded = VietnameseText.fold(VietnameseText.normalize(text.trim())).toLowerCase(Locale.ROOT);
        return String.join(" ", VietnameseText.words(folded));
    }

    // ---------- Dùng khi tìm kiếm ----------

    /**
     * Id doanh nghiệp ghim cho truy vấn, theo thứ tự; rỗng nếu từ khóa không có ghim.
     * Không bao giờ ném lỗi: đọc bảng ghim lỗi (vd. chưa có bảng) thì bỏ qua ghim, tìm kiếm vẫn chạy bình thường.
     * NOT_SUPPORTED: không dùng chung giao dịch với tìm kiếm, để lỗi ở đây không làm hỏng giao dịch bên ngoài.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<Long> pinnedListingIds(String query) {
        String key = key(query);
        if (key.isEmpty()) {
            return List.of();
        }
        try {
            Map<String, List<Long>> current = cache;
            if (current == null) {
                current = reload();
            }
            return current.getOrDefault(key, List.of());
        } catch (RuntimeException e) {
            LOG.warn("Không đọc được bảng ghim kết quả tìm kiếm, bỏ qua ghim: {}", e.getMessage());
            return List.of();
        }
    }

    private synchronized Map<String, List<Long>> reload() {
        Map<String, List<Long>> map = new LinkedHashMap<>();
        for (SearchPin pin : repository.findAllByOrderByPositionAscIdAsc()) {
            List<Long> ids = map.computeIfAbsent(key(pin.getKeyword()), k -> new ArrayList<>());
            if (!ids.contains(pin.getListingId())) {
                ids.add(pin.getListingId());
            }
        }
        map.replaceAll((k, ids) -> List.copyOf(ids));
        cache = Map.copyOf(map);
        return cache;
    }

    private void invalidate() {
        cache = null;
    }

    // ---------- Trang quản trị ----------

    @Transactional(readOnly = true)
    public List<PinGroup> groups() {
        List<SearchPin> pins = repository.findAllByOrderByPositionAscIdAsc();
        Map<Long, Map<String, Object>> listings = loadListings(pins.stream().map(SearchPin::getListingId).collect(Collectors.toSet()));
        Map<String, List<SearchPin>> byKey = new LinkedHashMap<>();
        for (SearchPin pin : pins) {
            byKey.computeIfAbsent(key(pin.getKeyword()), k -> new ArrayList<>()).add(pin);
        }
        return byKey
            .entrySet()
            .stream()
            .map(entry -> {
                List<PinItem> items = entry
                    .getValue()
                    .stream()
                    .map(pin -> {
                        Map<String, Object> l = listings.getOrDefault(pin.getListingId(), Map.of());
                        return new PinItem(
                            pin.getId(),
                            pin.getListingId(),
                            (String) l.get("name"),
                            (String) l.get("slug"),
                            (String) l.get("tax_code"),
                            (String) l.get("status"),
                            pin.getPosition()
                        );
                    })
                    .toList();
                return new PinGroup(entry.getValue().getFirst().getKeyword(), entry.getKey(), items);
            })
            .sorted(Comparator.comparing(PinGroup::key))
            .toList();
    }

    public PinGroup addPin(NewPin request) {
        String key = key(request.keyword());
        if (key.isEmpty() || request.listingId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cần từ khóa và doanh nghiệp");
        }
        Integer exists = jdbcTemplate.queryForObject("select count(*) from listing where id = ?", Integer.class, request.listingId());
        if (exists == null || exists == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không có doanh nghiệp " + request.listingId());
        }
        List<SearchPin> group = pinsOf(key);
        if (group.stream().anyMatch(pin -> pin.getListingId().equals(request.listingId()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Doanh nghiệp này đã được ghim cho từ khóa");
        }
        if (group.size() >= MAX_PINS_PER_KEYWORD) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tối đa " + MAX_PINS_PER_KEYWORD + " doanh nghiệp cho một từ khóa");
        }
        SearchPin pin = new SearchPin();
        // Cùng nhóm thì dùng chung cách viết từ khóa đã có (giữ dấu)
        pin.setKeyword(group.isEmpty() ? request.keyword().trim() : group.getFirst().getKeyword());
        pin.setListingId(request.listingId());
        pin.setPosition(group.stream().mapToInt(SearchPin::getPosition).max().orElse(0) + 1);
        pin.setCreatedAt(Instant.now());
        pin.setCreatedBy(SecurityUtils.getCurrentUserLogin().orElse("system"));
        repository.saveAndFlush(pin);
        invalidate();
        LOG.info("Ghim doanh nghiệp {} cho từ khóa '{}' (bởi {})", pin.getListingId(), pin.getKeyword(), pin.getCreatedBy());
        return group(key);
    }

    public void removePin(Long id) {
        repository.findById(id).ifPresent(pin -> {
            repository.delete(pin);
            repository.flush();
            LOG.info("Bỏ ghim doanh nghiệp {} khỏi từ khóa '{}'", pin.getListingId(), pin.getKeyword());
        });
        invalidate();
    }

    /** Xóa cả từ khóa (mọi doanh nghiệp ghim của nó). */
    public void removeGroup(String keyword) {
        List<SearchPin> group = pinsOf(key(keyword));
        repository.deleteAll(group);
        repository.flush();
        invalidate();
        LOG.info("Xóa từ khóa ghim '{}' ({} doanh nghiệp)", keyword, group.size());
    }

    /** Xếp lại: các id ghim theo thứ tự mới, phải cùng một từ khóa. */
    public PinGroup reorder(Reorder request) {
        List<SearchPin> pins = repository.findAllById(request.pinIds());
        if (pins.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không có ghim nào");
        }
        String key = key(pins.getFirst().getKeyword());
        if (
            pins
                .stream()
                .map(pin -> key(pin.getKeyword()))
                .anyMatch(k -> !k.equals(key))
        ) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Các ghim phải cùng một từ khóa");
        }
        Map<Long, SearchPin> byId = pins.stream().collect(Collectors.toMap(SearchPin::getId, pin -> pin));
        int position = 1;
        for (Long id : request.pinIds()) {
            SearchPin pin = byId.get(id);
            if (pin != null) {
                pin.setPosition(position++);
            }
        }
        repository.saveAllAndFlush(pins);
        invalidate();
        return group(key);
    }

    private List<SearchPin> pinsOf(String key) {
        return repository
            .findAllByOrderByPositionAscIdAsc()
            .stream()
            .filter(pin -> key(pin.getKeyword()).equals(key))
            .toList();
    }

    private PinGroup group(String key) {
        return groups()
            .stream()
            .filter(group -> group.key().equals(key))
            .findFirst()
            .orElse(new PinGroup(key, key, List.of()));
    }

    private Map<Long, Map<String, Object>> loadListings(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        NamedParameterJdbcTemplate named = new NamedParameterJdbcTemplate(jdbcTemplate);
        return named
            .queryForList("select id, name, slug, tax_code, status from listing where id in (:ids)", new MapSqlParameterSource("ids", ids))
            .stream()
            .collect(Collectors.toMap(row -> ((Number) row.get("id")).longValue(), row -> row, (a, b) -> a));
    }
}
