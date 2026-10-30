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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Predicate;
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
    static final int MAX_FORMER = 50;
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

    /**
     * Một dòng bảng location_conversion: xã cũ (mã xã trống: cả huyện, vd huyện đảo → đặc khu) → xã mới.
     * Mã giống mã cuối slug: tỉnh 2 số, huyện 3 số, xã 5 số.
     */
    record Conversion(
        String oldWardCode,
        String oldWardName,
        String oldDistrictCode,
        String oldDistrictName,
        String oldProvinceCode,
        String oldProvinceName,
        String newProvinceCode,
        String newWardCode,
        boolean partial
    ) {}

    private record Conversions(List<Conversion> rows, Instant loadedAt) {}

    /** Đơn vị cũ đang gom khi tìm theo tên cũ: mã đích → số dòng chuyển đổi sang đó (huyện cũ: số xã cũ nhập vào). */
    private record FormerHit(String name, String type, String parentName, String provinceName, Map<String, Integer> targetCodes) {}

    /** Danh sách đơn vị đã sắp theo tên, kèm số doanh nghiệp, và lúc đọc. */
    private record Snapshot(List<PublicLocationDTO> all, Instant loadedAt) {}

    /** Khóa sắp xếp tính sẵn: tên riêng, tên đầy đủ, tên tỉnh (so bằng Collator tiếng Việt rất chậm nếu so lặp lại). */
    private record Keyed(PublicLocationDTO dto, CollationKey proper, CollationKey name, CollationKey province) {}

    private final JdbcTemplate jdbcTemplate;
    private final ElasticsearchClient elasticsearchClient;
    /** Slug cũ trên website WordPress → wp_term_id (đơn vị đã đổi slug khi chuẩn hóa địa giới 2025). */
    private final Map<String, Long> slugAliases;
    private volatile Snapshot snapshot;
    private volatile Conversions conversions;

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
     * Đơn vị bộ mới đổi tên sau này (vd xã lên phường: {@code xa-bo-ha-07294-2025} → {@code phuong-bo-ha-07294-2025}) tìm theo
     * mã trong slug, cũng ra slug mới.
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
        if (found.isEmpty()) {
            found = findNewByCode(all, slugOrId);
        }
        // Đơn vị cũ (trước 01/7/2025, cả quận/huyện đã bỏ) → đơn vị mới đang chứa nó: slug khác slug hỏi, FE chuyển hướng 301
        if (found.isPresent() && !found.get().isNew()) {
            PublicLocationDTO old = found.get();
            found = newUnitOf(all, loadConversions(), old, this::sharedListings).or(() -> Optional.of(old));
        }
        return found.map(l -> detail(all, l));
    }

    /**
     * Tìm theo tên đơn vị cũ (xã, quận/huyện, tỉnh trước 01/7/2025), kèm đơn vị mới hiện chứa nó: người dùng còn nhớ
     * địa chỉ cũ ("Dịch Vọng Hậu", "Quận Cầu Giấy") vẫn tìm được phường, xã mới.
     */
    public List<PublicLocationDTO.Former> former(String query, int limit) {
        return former(loadAll(), loadConversions(), query, Math.clamp(limit, 1, MAX_FORMER));
    }

    /** Các đơn vị cũ đã nhập vào một đơn vị mới (xã mới ← các xã cũ, tỉnh mới ← các tỉnh cũ); không tìm thấy → empty. */
    public Optional<List<PublicLocationDTO.Origin>> origins(String slugOrId) {
        List<PublicLocationDTO> all = loadAll();
        return find(all, slugOrId)
            .or(() -> findNewByCode(all, slugOrId))
            .map(l -> origins(all, loadConversions(), l));
    }

    /** Đơn vị cũ nay thuộc đơn vị mới nào (xã cũ → xã mới, huyện cũ → các xã mới, tỉnh cũ → tỉnh mới); không tìm thấy → empty. */
    public Optional<List<PublicLocationDTO>> successors(String slugOrId) {
        List<PublicLocationDTO> all = loadAll();
        return find(all, slugOrId).map(l -> successors(all, loadConversions(), l));
    }

    /** Cây tra cứu của trang quản trị: mọi đơn vị bộ mới hoặc bộ cũ (không gồm "QUỐC TẾ" không có mã), kèm cờ có trong bảng chuyển đổi. */
    public List<PublicLocationDTO.Node> nodes(String generation) {
        return nodes(loadAll(), loadConversions(), checkedGeneration(generation));
    }

    // ---------- Hàm thuần (kiểm thử được) ----------

    /** Slug bộ mới không còn (đơn vị đã đổi tên) → đơn vị bộ mới cùng mã ({@code ...-07294-2025}). */
    static Optional<PublicLocationDTO> findNewByCode(List<PublicLocationDTO> all, String slug) {
        if (!StringUtils.hasText(slug) || !AdministrativeUnits.isNew(slug.trim())) {
            return Optional.empty();
        }
        String code = AdministrativeUnits.codeFromSlug(slug.trim());
        return code == null
            ? Optional.empty()
            : all
                  .stream()
                  .filter(l -> AdministrativeUnits.isNew(l.slug()) && code.equals(AdministrativeUnits.codeFromSlug(l.slug())))
                  .findFirst();
    }

    /** Đơn vị bộ mới theo mã cuối slug (tỉnh 2 số, xã 5 số nên không lẫn nhau). */
    private static Map<String, PublicLocationDTO> newByCode(List<PublicLocationDTO> all) {
        Map<String, PublicLocationDTO> result = new HashMap<>();
        for (PublicLocationDTO l : all) {
            String code = l.isNew() ? AdministrativeUnits.codeFromSlug(l.slug()) : null;
            if (code != null) {
                result.putIfAbsent(code, l);
            }
        }
        return result;
    }

    /**
     * Đơn vị bộ mới đang chứa một đơn vị cũ, theo bảng chuyển đổi: xã cũ → xã mới; quận/huyện, tỉnh cũ → tỉnh mới. Xã không
     * có trong bảng → theo huyện, rồi tỉnh chứa nó. Xã cũ bị chia: xã mới cùng tên riêng, không có thì nơi có nhiều doanh
     * nghiệp của xã cũ nhất ({@code shared}: id xã mới → số doanh nghiệp gắn cả hai), rồi nơi nhận toàn bộ.
     */
    static Optional<PublicLocationDTO> newUnitOf(
        List<PublicLocationDTO> all,
        List<Conversion> conversions,
        PublicLocationDTO old,
        BiFunction<PublicLocationDTO, List<PublicLocationDTO>, Map<Long, Long>> shared
    ) {
        Map<Long, PublicLocationDTO> byId = new HashMap<>();
        all.forEach(l -> byId.put(l.id(), l));
        Map<String, PublicLocationDTO> newByCode = newByCode(all);
        Set<Long> seen = new HashSet<>();
        for (
            PublicLocationDTO unit = old;
            unit != null && seen.add(unit.id());
            unit = unit.parentId() == null ? null : byId.get(unit.parentId())
        ) {
            String code = AdministrativeUnits.codeFromSlug(unit.slug());
            if (unit.isNew() || code == null) {
                continue;
            }
            String type = unit.type();
            String unitName = unit.name();
            List<PublicLocationDTO> targets = conversions
                .stream()
                .filter(
                    c ->
                        switch (type) {
                            case AdministrativeUnits.WARD -> code.equals(c.oldWardCode());
                            case AdministrativeUnits.DISTRICT -> code.equals(c.oldDistrictCode());
                            case AdministrativeUnits.PROVINCE -> code.equals(c.oldProvinceCode());
                            default -> false;
                        }
                )
                .sorted(Comparator.comparing(Conversion::partial))
                .map(c -> newByCode.get(AdministrativeUnits.WARD.equals(type) ? c.newWardCode() : c.newProvinceCode()))
                .filter(Objects::nonNull)
                .toList();
            if (targets.isEmpty()) {
                continue;
            }
            Optional<PublicLocationDTO> sameName = targets
                .stream()
                .filter(t -> sameProperName(t.name(), unitName))
                .findFirst();
            if (sameName.isPresent() || targets.size() < 2) {
                return sameName.or(() -> targets.stream().findFirst());
            }
            Map<Long, Long> counts = shared.apply(unit, targets);
            // Sắp ổn định: số bằng nhau giữ thứ tự cũ (nhận toàn bộ trước)
            return targets
                .stream()
                .sorted(Comparator.comparingLong(t -> -counts.getOrDefault(t.id(), 0L)))
                .findFirst();
        }
        return Optional.empty();
    }

    /**
     * Đơn vị cũ có tên khớp mọi từ khóa (khớp đầu từ, không dấu), mỗi đơn vị kèm đơn vị mới chứa nó. Bỏ đơn vị chỉ đổi cấp mà giữ tên riêng (Thị trấn Đông Anh → Xã Đông Anh): tìm thường đã ra. Xếp: tên riêng
     * đúng từ khóa trước, rồi tỉnh, huyện, xã, rồi nhiều doanh nghiệp.
     */
    static List<PublicLocationDTO.Former> former(List<PublicLocationDTO> all, List<Conversion> conversions, String query, int limit) {
        List<String> words = VietnameseText.words(StringUtils.hasText(query) ? fold(query) : "");
        if (words.isEmpty()) {
            return List.of();
        }
        Map<String, Boolean> matched = new HashMap<>();
        Predicate<String> matches = name -> name != null && matched.computeIfAbsent(name, n -> matchesAll(fold(n), words));
        Map<String, FormerHit> hits = new LinkedHashMap<>();
        for (Conversion c : conversions) {
            if (c.oldWardCode() != null && matches.test(c.oldWardName())) {
                hits.computeIfAbsent("w" + c.oldWardCode(), k ->
                    new FormerHit(c.oldWardName(), AdministrativeUnits.WARD, c.oldDistrictName(), c.oldProvinceName(), new HashMap<>())
                )
                    .targetCodes()
                    .merge(c.newWardCode(), 1, Integer::sum);
            }
            if (matches.test(c.oldDistrictName())) {
                hits.computeIfAbsent("d" + c.oldDistrictCode(), k ->
                    new FormerHit(c.oldDistrictName(), AdministrativeUnits.DISTRICT, null, c.oldProvinceName(), new HashMap<>())
                )
                    .targetCodes()
                    .merge(c.newWardCode(), 1, Integer::sum);
            }
            if (matches.test(c.oldProvinceName())) {
                hits.computeIfAbsent("p" + c.oldProvinceCode(), k ->
                    new FormerHit(c.oldProvinceName(), AdministrativeUnits.PROVINCE, null, null, new HashMap<>())
                )
                    .targetCodes()
                    .merge(c.newProvinceCode(), 1, Integer::sum);
            }
        }
        Map<String, PublicLocationDTO> newByCode = newByCode(all);
        String exact = String.join(" ", words);
        List<String> typeOrder = List.of(AdministrativeUnits.PROVINCE, AdministrativeUnits.DISTRICT, AdministrativeUnits.WARD);
        return hits
            .values()
            .stream()
            .map(h -> {
                // Nơi nhận nhiều xã cũ nhất trước (Quận Cầu Giấy → Phường Cầu Giấy), rồi nhiều doanh nghiệp
                List<PublicLocationDTO> targets = h
                    .targetCodes()
                    .entrySet()
                    .stream()
                    .filter(e -> newByCode.containsKey(e.getKey()))
                    .sorted(
                        Comparator.comparing((Map.Entry<String, Integer> e) -> -e.getValue()).thenComparing(
                            e -> -newByCode.get(e.getKey()).listingCount()
                        )
                    )
                    .map(e -> newByCode.get(e.getKey()))
                    .toList();
                return new PublicLocationDTO.Former(h.name(), h.type(), h.parentName(), h.provinceName(), targets);
            })
            .filter(f -> !f.targets().isEmpty())
            .filter(f -> f.targets().size() > 1 || !sameProperName(f.name(), f.targets().getFirst().name()))
            .sorted(
                Comparator.comparing((PublicLocationDTO.Former f) -> !fold(properName(f.name())).equals(exact))
                    .thenComparing(f -> typeOrder.indexOf(f.type()))
                    .thenComparing(f -> -f.targets().stream().mapToLong(PublicLocationDTO::listingCount).sum())
            )
            .limit(limit)
            .toList();
    }

    private static boolean sameProperName(String a, String b) {
        return AdministrativeUnits.nameKey(properName(a)).equals(AdministrativeUnits.nameKey(properName(b)));
    }

    /** Đơn vị bộ cũ theo cấp và mã cuối slug ("ward:00167"): mã các cấp có thể trùng nhau giữa tỉnh, huyện. */
    private static Map<String, PublicLocationDTO> oldByCode(List<PublicLocationDTO> all) {
        Map<String, PublicLocationDTO> result = new HashMap<>();
        for (PublicLocationDTO l : all) {
            String code = l.isNew() ? null : AdministrativeUnits.codeFromSlug(l.slug());
            if (code != null) {
                result.putIfAbsent(l.type() + ":" + code, l);
            }
        }
        return result;
    }

    /**
     * Đơn vị cũ đã nhập vào đơn vị mới: xã mới ← các xã cũ (hoặc cả huyện cũ, vd huyện đảo), tỉnh mới ← các tỉnh cũ.
     * Kèm id đơn vị cũ trong bảng location (nếu có) để trang quản trị mở tới.
     */
    static List<PublicLocationDTO.Origin> origins(List<PublicLocationDTO> all, List<Conversion> conversions, PublicLocationDTO unit) {
        String code = AdministrativeUnits.codeFromSlug(unit.slug());
        if (!unit.isNew() || code == null) {
            return List.of();
        }
        Map<String, PublicLocationDTO> old = oldByCode(all);
        boolean province = AdministrativeUnits.PROVINCE.equals(unit.type());
        Map<String, PublicLocationDTO.Origin> result = new LinkedHashMap<>();
        for (Conversion c : conversions) {
            String type;
            String oldCode;
            String name;
            String parentName;
            String provinceName;
            if (province && code.equals(c.newProvinceCode())) {
                type = AdministrativeUnits.PROVINCE;
                oldCode = c.oldProvinceCode();
                name = c.oldProvinceName();
                parentName = null;
                provinceName = null;
            } else if (!province && code.equals(c.newWardCode()) && c.oldWardCode() == null) {
                type = AdministrativeUnits.DISTRICT;
                oldCode = c.oldDistrictCode();
                name = c.oldDistrictName();
                parentName = null;
                provinceName = c.oldProvinceName();
            } else if (!province && code.equals(c.newWardCode())) {
                type = AdministrativeUnits.WARD;
                oldCode = c.oldWardCode();
                name = c.oldWardName();
                parentName = c.oldDistrictName();
                provinceName = c.oldProvinceName();
            } else {
                continue;
            }
            PublicLocationDTO match = old.get(type + ":" + oldCode);
            boolean partial = !province && c.partial();
            result.putIfAbsent(
                type + ":" + oldCode,
                new PublicLocationDTO.Origin(match == null ? null : match.id(), oldCode, name, type, parentName, provinceName, partial)
            );
        }
        return List.copyOf(result.values());
    }

    /**
     * Đơn vị mới hiện chứa một đơn vị cũ, theo bảng chuyển đổi: xã cũ → xã mới (nhiều nếu bị chia), quận/huyện cũ → các xã
     * mới (nơi nhận nhiều xã cũ nhất trước), tỉnh cũ → tỉnh mới. Không có trong bảng hoặc là đơn vị mới → rỗng.
     */
    static List<PublicLocationDTO> successors(List<PublicLocationDTO> all, List<Conversion> conversions, PublicLocationDTO unit) {
        String code = AdministrativeUnits.codeFromSlug(unit.slug());
        if (unit.isNew() || code == null || unit.type() == null) {
            return List.of();
        }
        Map<String, Integer> counts = new HashMap<>();
        for (Conversion c : conversions) {
            String target = switch (unit.type()) {
                case AdministrativeUnits.WARD -> code.equals(c.oldWardCode()) ? c.newWardCode() : null;
                case AdministrativeUnits.DISTRICT -> code.equals(c.oldDistrictCode()) ? c.newWardCode() : null;
                case AdministrativeUnits.PROVINCE -> code.equals(c.oldProvinceCode()) ? c.newProvinceCode() : null;
                default -> null;
            };
            if (target != null) {
                counts.merge(target, 1, Integer::sum);
            }
        }
        Map<String, PublicLocationDTO> newByCode = newByCode(all);
        return counts
            .entrySet()
            .stream()
            .filter(e -> newByCode.containsKey(e.getKey()))
            .sorted(
                Comparator.comparing((Map.Entry<String, Integer> e) -> -e.getValue()).thenComparing(
                    e -> -newByCode.get(e.getKey()).listingCount()
                )
            )
            .map(e -> newByCode.get(e.getKey()))
            .toList();
    }

    /** Nút cây cho trang quản trị: bộ mới ({@code new}), bộ cũ ({@code old}) hoặc cả hai; bỏ đơn vị không có mã (QUỐC TẾ). */
    static List<PublicLocationDTO.Node> nodes(List<PublicLocationDTO> all, List<Conversion> conversions, String generation) {
        Set<String> mapped = new HashSet<>();
        for (Conversion c : conversions) {
            mapped.add("new:province:" + c.newProvinceCode());
            mapped.add("new:ward:" + c.newWardCode());
            mapped.add("old:province:" + c.oldProvinceCode());
            mapped.add("old:district:" + c.oldDistrictCode());
            if (c.oldWardCode() != null) {
                mapped.add("old:ward:" + c.oldWardCode());
            }
        }
        return scope(all, generation, null, false)
            .stream()
            .map(l -> {
                String code = AdministrativeUnits.codeFromSlug(l.slug());
                String key = (l.isNew() ? "new:" : "old:") + l.type() + ":" + code;
                return new PublicLocationDTO.Node(l.id(), code, l.name(), l.type(), l.parentId(), l.listingCount(), mapped.contains(key));
            })
            .filter(n -> n.code() != null)
            .toList();
    }

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

    /** Số doanh nghiệp gắn cả đơn vị cũ lẫn từng đơn vị mới (id đơn vị mới → số), để chọn nơi nhận phần lớn xã cũ bị chia. */
    private Map<Long, Long> sharedListings(PublicLocationDTO old, List<PublicLocationDTO> targets) {
        Map<Long, Long> result = new HashMap<>();
        String placeholders = String.join(",", Collections.nCopies(targets.size(), "?"));
        List<Object> args = new ArrayList<>();
        args.add(old.id());
        targets.forEach(t -> args.add(t.id()));
        jdbcTemplate.query(
            "select r2.location_id, count(*) from rel_listing__location r1 join rel_listing__location r2 on r2.listing_id = r1.listing_id " +
                "where r1.location_id = ? and r2.location_id in (" +
                placeholders +
                ") group by r2.location_id",
            rs -> {
                result.put(rs.getLong(1), rs.getLong(2));
            },
            args.toArray()
        );
        return result;
    }

    /** Bảng chuyển đổi đơn vị cũ → mới (~10.600 dòng, gần như không đổi): đọc lại sau {@link #CACHE_TTL}. */
    List<Conversion> loadConversions() {
        Conversions current = conversions;
        if (current != null && Instant.now().isBefore(current.loadedAt().plus(CACHE_TTL))) {
            return current.rows();
        }
        List<Conversion> rows = jdbcTemplate.query(
            "select old_ward_code, old_ward_name, old_district_code, old_district_name, old_province_code, old_province_name, " +
                "new_province_code, new_ward_code, partial_merge from location_conversion order by id",
            (rs, n) ->
                new Conversion(
                    rs.getString(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getString(4),
                    rs.getString(5),
                    rs.getString(6),
                    rs.getString(7),
                    rs.getString(8),
                    rs.getBoolean(9)
                )
        );
        Conversions loaded = new Conversions(List.copyOf(rows), Instant.now());
        conversions = loaded;
        return loaded.rows();
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
