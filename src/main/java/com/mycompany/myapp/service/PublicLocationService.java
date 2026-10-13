package com.mycompany.myapp.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import com.mycompany.myapp.repository.search.VietnameseText;
import com.mycompany.myapp.service.dto.publicapi.PublicLocationDTO;
import com.mycompany.myapp.service.dto.publicapi.PublicPageDTO;
import com.mycompany.myapp.service.dto.publicapi.PublicTermDetailDTO;
import com.mycompany.myapp.service.dvhc.AdministrativeUnits;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.CollationKey;
import java.text.Collator;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Mục lục tỉnh thành theo chữ cái, kèm số doanh nghiệp.
 * <ul>
 *   <li>Chữ cái đầu tính theo <b>tên riêng</b>: bỏ tiền tố "Tỉnh", "Thành phố", "Quận", "Huyện", "Thị xã", "Phường", "Xã",
 *   "Thị trấn", "Đặc khu" ("Xã Châu Thới" → C). Bỏ dấu (Ô → O, Ư → U) nhưng <b>giữ Đ</b> là chữ riêng, đứng sau D.</li>
 *   <li>Số doanh nghiệp: một phép đếm gộp (terms aggregation) trên {@code locationIds} của index listing (đã gồm các cấp cha),
 *   nên là số của cả đơn vị con; chỉ đếm doanh nghiệp đã xuất bản như mục lục ngành nghề.</li>
 *   <li>Danh sách đơn vị (đã sắp theo tên) và số doanh nghiệp giữ trong bộ nhớ {@link #CACHE_TTL}: sửa tên địa phương hay
 *   reindex listing thì tối đa 10 phút sau mới thấy.</li>
 *   <li>Địa giới: {@code new} (mặc định, từ 01/7/2025), {@code old}, {@code all}; cấp: province, district, ward.</li>
 * </ul>
 */
@Service
public class PublicLocationService {

    private static final Logger LOG = LoggerFactory.getLogger(PublicLocationService.class);

    static final String PUBLISHED = "publish";
    static final String SLUG_ALIASES_FILE = "config/location-slug-aliases.csv";
    static final Duration CACHE_TTL = Duration.ofMinutes(10);
    static final int MAX_PAGE_SIZE = 500;
    static final String OTHER_LETTER = "#";
    static final String D_STROKE = "Đ";
    static final Set<String> GENERATIONS = Set.of("new", "old", "all");
    static final Set<String> LEVELS = Set.of(AdministrativeUnits.PROVINCE, AdministrativeUnits.DISTRICT, AdministrativeUnits.WARD);

    private static final Pattern UNIT_PREFIX = Pattern.compile(
        "^(tỉnh|thành phố|quận|huyện|thị xã|phường|xã|thị trấn|đặc khu)\\s+",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi");
    private static final Set<String> SORTS = Set.of("name", "listingCount");

    /** Một dòng bảng location. */
    record Row(long id, String name, String slug, String type, Long parentId) {}

    /** Danh sách đơn vị đã sắp theo tên, kèm số doanh nghiệp, và lúc đọc. */
    private record Snapshot(List<PublicLocationDTO> all, Instant loadedAt) {}

    /** Khóa sắp xếp tính sẵn: tên riêng, tên đầy đủ, tên tỉnh (so bằng Collator tiếng Việt rất chậm nếu so lặp lại). */
    private record Keyed(PublicLocationDTO dto, CollationKey proper, CollationKey name, CollationKey province) {}

    private final JdbcTemplate jdbcTemplate;
    private final ElasticsearchClient elasticsearchClient;
    /** Slug cũ trên website WordPress → wp_term_id (đơn vị đã đổi slug khi chuẩn hóa địa giới 2025). */
    private final Map<String, Long> slugAliases;
    private volatile Snapshot snapshot;

    public PublicLocationService(DataSource dataSource, ElasticsearchClient elasticsearchClient) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.elasticsearchClient = elasticsearchClient;
        this.slugAliases = loadSlugAliases();
    }

    public List<PublicLocationDTO.Letter> letters(String generation, String level, boolean hideEmpty) {
        return letters(scope(loadAll(), checkedGeneration(generation), checkedLevel(level), hideEmpty));
    }

    public PublicPageDTO<PublicLocationDTO> locations(
        String letter,
        String query,
        String generation,
        String level,
        boolean hideEmpty,
        Pageable pageable
    ) {
        return filter(scope(loadAll(), checkedGeneration(generation), checkedLevel(level), hideEmpty), letter, query, pageable);
    }

    /**
     * Chi tiết một đơn vị theo slug hoặc id, kèm các cấp cha (breadcrumb) và cấp con trực tiếp (đã sắp theo tên).
     * Slug cũ của website WordPress (166 đơn vị đổi slug năm 2025) cũng tìm được: kết quả mang slug mới, FE chuyển hướng 301.
     */
    public Optional<PublicTermDetailDTO> location(String slugOrId) {
        List<PublicLocationDTO> all = loadAll();
        Optional<PublicLocationDTO> found = find(all, slugOrId);
        if (found.isEmpty() && StringUtils.hasText(slugOrId)) {
            Long wpTermId = slugAliases.get(slugOrId.trim());
            if (wpTermId != null) {
                found = jdbcTemplate
                    .queryForList("select id from location where wp_term_id = ?", Long.class, wpTermId)
                    .stream()
                    .findFirst()
                    .flatMap(id -> find(all, String.valueOf(id)));
            }
        }
        return found.map(l -> detail(all, l));
    }

    // ---------- Hàm thuần (kiểm thử được) ----------

    /** Tìm theo slug trước (đúng nguyên văn), không có thì theo id. */
    static Optional<PublicLocationDTO> find(List<PublicLocationDTO> all, String slugOrId) {
        if (!StringUtils.hasText(slugOrId)) {
            return Optional.empty();
        }
        String key = slugOrId.trim();
        Optional<PublicLocationDTO> bySlug = all
            .stream()
            .filter(l -> key.equals(l.slug()))
            .findFirst();
        if (bySlug.isPresent() || !key.chars().allMatch(Character::isDigit) || key.length() > 18) {
            return bySlug;
        }
        long id = Long.parseLong(key);
        return all
            .stream()
            .filter(l -> l.id() == id)
            .findFirst();
    }

    /** Danh sách vào đã sắp theo tên nên cấp con giữ thứ tự đó. */
    static PublicTermDetailDTO detail(List<PublicLocationDTO> all, PublicLocationDTO location) {
        Map<Long, PublicLocationDTO> byId = new HashMap<>();
        all.forEach(l -> byId.put(l.id(), l));
        List<PublicTermDetailDTO.Ref> ancestors = new ArrayList<>();
        Set<Long> seen = new HashSet<>(Set.of(location.id()));
        PublicLocationDTO parent = location.parentId() == null ? null : byId.get(location.parentId());
        while (parent != null && seen.add(parent.id())) {
            ancestors.addFirst(ref(parent));
            parent = parent.parentId() == null ? null : byId.get(parent.parentId());
        }
        List<PublicTermDetailDTO.Ref> children = all
            .stream()
            .filter(l -> location.id().equals(l.parentId()))
            .map(PublicLocationService::ref)
            .toList();
        return new PublicTermDetailDTO(
            location.id(),
            location.name(),
            location.slug(),
            null,
            location.type(),
            location.isNew(),
            location.listingCount(),
            List.copyOf(ancestors),
            children
        );
    }

    private static PublicTermDetailDTO.Ref ref(PublicLocationDTO l) {
        return new PublicTermDetailDTO.Ref(l.id(), l.name(), l.slug(), l.listingCount());
    }

    /** Dòng "old_slug,wp_term_id"; bỏ dòng trống, dòng chú thích (#) và dòng tiêu đề. */
    static Map<String, Long> parseSlugAliases(List<String> lines) {
        Map<String, Long> result = new HashMap<>();
        for (String line : lines) {
            String[] parts = line.trim().split(",");
            if (parts.length == 2 && !parts[0].startsWith("#") && parts[1].trim().chars().allMatch(Character::isDigit)) {
                result.put(parts[0].trim(), Long.valueOf(parts[1].trim()));
            }
        }
        return Map.copyOf(result);
    }

    private static Map<String, Long> loadSlugAliases() {
        try (var in = new ClassPathResource(SLUG_ALIASES_FILE).getInputStream()) {
            return parseSlugAliases(new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList());
        } catch (IOException e) {
            LOG.warn("Không đọc được {}: slug cũ của địa phương sẽ không chuyển hướng được ({})", SLUG_ALIASES_FILE, e.getMessage());
            return Map.of();
        }
    }

    static List<PublicLocationDTO> scope(List<PublicLocationDTO> all, String generation, String level, boolean hideEmpty) {
        return all
            .stream()
            .filter(l -> "all".equals(generation) || "new".equals(generation) == l.isNew())
            .filter(l -> level == null || level.equals(l.type()))
            .filter(l -> !hideEmpty || l.listingCount() > 0)
            .toList();
    }

    /** Các chữ cái có đơn vị: theo bảng chữ cái tiếng Việt (Đ sau D), "#" cuối cùng. */
    static List<PublicLocationDTO.Letter> letters(List<PublicLocationDTO> locations) {
        Map<String, Long> counts = locations
            .stream()
            .collect(Collectors.groupingBy(PublicLocationDTO::letter, () -> new TreeMap<>(LETTER_ORDER), Collectors.counting()));
        return counts
            .entrySet()
            .stream()
            .map(e -> new PublicLocationDTO.Letter(e.getKey(), e.getValue()))
            .toList();
    }

    /** A..D, Đ, E..Z, rồi "#". */
    static final Comparator<String> LETTER_ORDER = Comparator.comparing(PublicLocationService::letterRank);

    private static double letterRank(String letter) {
        if (OTHER_LETTER.equals(letter)) {
            return 1000;
        }
        if (D_STROKE.equals(letter)) {
            return 'D' + 0.5;
        }
        return letter.charAt(0);
    }

    static PublicPageDTO<PublicLocationDTO> filter(List<PublicLocationDTO> scoped, String letter, String query, Pageable pageable) {
        String wanted = StringUtils.hasText(letter) ? checkedLetter(letter) : null;
        List<String> words = VietnameseText.words(StringUtils.hasText(query) ? fold(query) : "");
        List<PublicLocationDTO> matched = scoped
            .stream()
            .filter(l -> wanted == null || wanted.equals(l.letter()))
            .filter(l -> words.isEmpty() || matchesAll(fold(l.name() + " " + nullToEmpty(l.provinceName())), words))
            .collect(Collectors.toCollection(ArrayList::new));
        Comparator<PublicLocationDTO> order = null;
        if (!words.isEmpty() && !sortsByCountFirst(pageable.getSort())) {
            // "thanh luong": Phường Thanh Lương (khớp trong tên) trước Xã Bắc Lương, Thanh Hóa (khớp nhờ tên tỉnh)
            Set<PublicLocationDTO> inName = Collections.newSetFromMap(new IdentityHashMap<>());
            matched
                .stream()
                .filter(l -> matchesAll(fold(l.name()), words))
                .forEach(inName::add);
            order = Comparator.comparingInt(l -> inName.contains(l) ? 0 : 1);
        }
        if (pageable.getSort().isSorted()) {
            // Danh sách vào đã theo tên: thứ tự theo tên chính là vị trí; sắp ổn định nên số bằng nhau vẫn giữ thứ tự tên
            Map<PublicLocationDTO, Integer> position = new IdentityHashMap<>();
            for (int i = 0; i < matched.size(); i++) {
                position.put(matched.get(i), i);
            }
            Comparator<PublicLocationDTO> sorted = comparator(pageable.getSort(), Comparator.comparingInt(position::get));
            order = order == null ? sorted : order.thenComparing(sorted);
        }
        if (order != null) {
            matched.sort(order);
        }
        int size = Math.clamp(pageable.getPageSize(), 1, MAX_PAGE_SIZE);
        int page = Math.max(pageable.getPageNumber(), 0);
        int from = (int) Math.min((long) page * size, matched.size());
        int to = Math.min(from + size, matched.size());
        return new PublicPageDTO<>(matched.subList(from, to), page, size, matched.size(), (matched.size() + size - 1) / size);
    }

    /** "Xã Châu Thới" → "Châu Thới". */
    static String properName(String name) {
        return name == null ? "" : UNIT_PREFIX.matcher(name.trim()).replaceFirst("").trim();
    }

    /** Chữ cái đầu của tên riêng: Đ giữ riêng; chữ khác bỏ dấu, in hoa; không phải chữ cái A–Z thì "#". */
    static String letterOf(String name) {
        String proper = properName(name);
        if (proper.isEmpty()) {
            return OTHER_LETTER;
        }
        String first = new String(Character.toChars(proper.codePointAt(0)));
        if ("đ".equalsIgnoreCase(first)) {
            return D_STROKE;
        }
        String folded = VietnameseText.fold(first).toUpperCase(Locale.ROOT);
        return folded.length() == 1 && folded.charAt(0) >= 'A' && folded.charAt(0) <= 'Z' ? folded : OTHER_LETTER;
    }

    /** "đ", "Đ" → "Đ"; "l" → "L"; "#" giữ nguyên; còn lại 400. */
    static String checkedLetter(String letter) {
        String trimmed = letter.trim();
        if (OTHER_LETTER.equals(trimmed)) {
            return OTHER_LETTER;
        }
        String result = letterOf(trimmed);
        if (trimmed.codePointCount(0, trimmed.length()) != 1 || OTHER_LETTER.equals(result)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "letter phải là một chữ cái A–Z, Đ hoặc #, nhận được: " + letter);
        }
        return result;
    }

    static String checkedGeneration(String generation) {
        String value = StringUtils.hasText(generation) ? generation.trim().toLowerCase(Locale.ROOT) : "new";
        if (!GENERATIONS.contains(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "generation phải là new, old hoặc all, nhận được: " + generation);
        }
        return value;
    }

    static String checkedLevel(String level) {
        if (!StringUtils.hasText(level)) {
            return null;
        }
        String value = level.trim().toLowerCase(Locale.ROOT);
        if (!LEVELS.contains(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "level phải là province, district hoặc ward, nhận được: " + level);
        }
        return value;
    }

    private static Comparator<PublicLocationDTO> comparator(Sort sort, Comparator<PublicLocationDTO> byName) {
        Comparator<PublicLocationDTO> result = null;
        for (Sort.Order order : sort) {
            if (!SORTS.contains(order.getProperty())) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Không sắp xếp được theo '" + order.getProperty() + "'. Dùng: " + SORTS
                );
            }
            Comparator<PublicLocationDTO> one = "name".equals(order.getProperty())
                ? byName
                : Comparator.comparingLong(PublicLocationDTO::listingCount);
            one = order.isDescending() ? one.reversed() : one;
            result = result == null ? one : result.thenComparing(one);
        }
        return result == null ? byName : result.thenComparing(byName);
    }

    private static boolean sortsByCountFirst(Sort sort) {
        return sort
            .stream()
            .findFirst()
            .map(order -> "listingCount".equals(order.getProperty()))
            .orElse(false);
    }

    private static String fold(String text) {
        return VietnameseText.fold(VietnameseText.normalize(text)).toLowerCase(Locale.ROOT);
    }

    /** Mỗi từ khóa là đầu của một từ trong tên: "dong anh" khớp "Đông Anh" nhưng không khớp "Đạo Thạnh, Đồng Tháp". */
    private static boolean matchesAll(String folded, List<String> words) {
        List<String> tokens = VietnameseText.words(folded);
        return words.stream().allMatch(word -> tokens.stream().anyMatch(token -> token.startsWith(word)));
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /**
     * Dựng DTO từ các dòng location và số doanh nghiệp, sắp theo chữ cái (như dãy nút: A–D, Đ, E–Z, # cuối), rồi tên riêng
     * (bỏ tiền tố), tên đầy đủ, tỉnh, id.
     */
    static List<PublicLocationDTO> toDtos(List<Row> rows, Map<Long, Long> counts) {
        Map<Long, Row> byId = new HashMap<>();
        rows.forEach(r -> byId.put(r.id(), r));
        List<PublicLocationDTO> result = new ArrayList<>(rows.size());
        for (Row row : rows) {
            Row parent = row.parentId() == null ? null : byId.get(row.parentId());
            // Đi ngược lên tỉnh (cây lỗi có vòng lặp cũng không treo)
            Row province = row;
            Set<Long> seen = new HashSet<>();
            while (province.parentId() != null && seen.add(province.id()) && byId.containsKey(province.parentId())) {
                province = byId.get(province.parentId());
            }
            result.add(
                new PublicLocationDTO(
                    row.id(),
                    row.name(),
                    row.slug(),
                    row.type(),
                    AdministrativeUnits.isNew(row.slug()),
                    letterOf(row.name()),
                    row.parentId(),
                    parent == null ? null : parent.name(),
                    province.name(),
                    counts.getOrDefault(row.id(), 0L)
                )
            );
        }
        return sortedByName(result);
    }

    private static List<PublicLocationDTO> sortedByName(List<PublicLocationDTO> locations) {
        Collator collator = Collator.getInstance(VIETNAMESE);
        return locations
            .stream()
            .map(l ->
                new Keyed(
                    l,
                    collator.getCollationKey(properName(l.name())),
                    collator.getCollationKey(nullToEmpty(l.name())),
                    collator.getCollationKey(nullToEmpty(l.provinceName()))
                )
            )
            .sorted(
                Comparator.comparing((Keyed k) -> k.dto().letter(), LETTER_ORDER)
                    .thenComparing(Keyed::proper)
                    .thenComparing(Keyed::name)
                    .thenComparing(Keyed::province)
                    .thenComparing(k -> k.dto().id())
            )
            .map(Keyed::dto)
            .toList();
    }

    // ---------- Đọc dữ liệu ----------

    List<PublicLocationDTO> loadAll() {
        Snapshot current = snapshot;
        if (current != null && Instant.now().isBefore(current.loadedAt().plus(CACHE_TTL))) {
            return current.all();
        }
        Map<Long, Long> counts = counts();
        List<Row> rows = jdbcTemplate.query("select id, name, slug, type, parent_id from location", (rs, n) ->
            new Row(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getObject(5, Long.class))
        );
        List<PublicLocationDTO> all = toDtos(rows, counts == null ? Map.of() : counts);
        if (counts != null) {
            snapshot = new Snapshot(all, Instant.now());
        } else if (current != null) {
            // ES lỗi: dùng tạm bản cũ (có số doanh nghiệp) thay vì hiện toàn số 0; lần sau thử lại
            return current.all();
        }
        return all;
    }

    /** Số doanh nghiệp theo id địa phương (gồm đơn vị con), từ Elasticsearch; lỗi thì null (số = 0) chứ không lỗi API. */
    private Map<Long, Long> counts() {
        try {
            var response = elasticsearchClient.search(
                s ->
                    s
                        .index("listing")
                        .size(0)
                        .query(q -> q.term(t -> t.field("status.keyword").value(PUBLISHED)))
                        .aggregations("locations", a -> a.terms(t -> t.field("locationIds").size(30_000))),
                Void.class
            );
            Map<Long, Long> loaded = new HashMap<>();
            for (LongTermsBucket bucket : response.aggregations().get("locations").lterms().buckets().array()) {
                loaded.put(bucket.key(), bucket.docCount());
            }
            return loaded;
        } catch (IOException | RuntimeException e) {
            LOG.warn("Không đếm được doanh nghiệp theo địa phương trên Elasticsearch: {}", e.getMessage());
            return null;
        }
    }
}
