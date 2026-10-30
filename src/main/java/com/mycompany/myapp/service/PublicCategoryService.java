package com.mycompany.myapp.service;

import com.mycompany.myapp.repository.CategoryPublicRepository;
import com.mycompany.myapp.repository.search.VietnameseText;
import com.mycompany.myapp.service.dto.publicapi.PublicCategoryDTO;
import com.mycompany.myapp.service.dto.publicapi.PublicPageDTO;
import com.mycompany.myapp.service.dto.publicapi.PublicTermDetailDTO;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

/**
 * API công khai ngành nghề cho FE (Next.js): mục lục theo chữ cái đầu, kèm số doanh nghiệp.
 * <p>
 * Chỉ ~2.400 ngành nên đọc hết rồi lọc, sắp xếp trong bộ nhớ. Chữ cái đầu bỏ dấu (Ô → O, Đ → D) để mục lục chỉ có
 * A–Z, giống FE cũ; tên ngành được giải mã {@code &amp;} (dữ liệu gốc WordPress).
 */
@Service
@Transactional(readOnly = true)
public class PublicCategoryService {

    static final String OTHER_LETTER = "#";
    static final String ROOT = "root";
    static final int MAX_PAGE_SIZE = 500;
    static final Map<String, Comparator<PublicCategoryDTO>> SORTS;

    private static final Comparator<String> VIETNAMESE = Comparator.nullsLast(Collator.getInstance(Locale.forLanguageTag("vi")));
    private static final Comparator<PublicCategoryDTO> BY_NAME = Comparator.comparing(PublicCategoryDTO::name, VIETNAMESE).thenComparing(
        PublicCategoryDTO::id
    );

    /** Theo mã ngành VSIC (A, 01, 011, 0111...); ngành chưa có mã xếp cuối. */
    private static final Comparator<PublicCategoryDTO> BY_CODE = Comparator.comparing(
        PublicCategoryDTO::code,
        Comparator.nullsLast(Comparator.<String>naturalOrder())
    ).thenComparing(BY_NAME);

    static {
        SORTS = Map.of(
            "name",
            BY_NAME,
            "listingCount",
            Comparator.comparingLong(PublicCategoryDTO::listingCount).thenComparing(BY_NAME),
            "code",
            BY_CODE
        );
    }

    private final CategoryPublicRepository categoryPublicRepository;

    public PublicCategoryService(CategoryPublicRepository categoryPublicRepository) {
        this.categoryPublicRepository = categoryPublicRepository;
    }

    /**
     * Các chữ cái có ngành, theo thứ tự A–Z, "#" cuối cùng.
     *
     * @param hideEmpty chỉ đếm ngành có doanh nghiệp.
     */
    public List<PublicCategoryDTO.Letter> letters(boolean hideEmpty) {
        return letters(loadAll(), hideEmpty);
    }

    static List<PublicCategoryDTO.Letter> letters(List<PublicCategoryDTO> categories, boolean hideEmpty) {
        Map<String, Long> counts = categories
            .stream()
            .filter(c -> !hideEmpty || c.listingCount() > 0)
            .collect(Collectors.groupingBy(PublicCategoryDTO::letter, TreeMap::new, Collectors.counting()));
        Long other = counts.remove(OTHER_LETTER);
        List<PublicCategoryDTO.Letter> result = new java.util.ArrayList<>(
            counts
                .entrySet()
                .stream()
                .map(e -> new PublicCategoryDTO.Letter(e.getKey(), e.getValue()))
                .toList()
        );
        if (other != null) {
            result.add(new PublicCategoryDTO.Letter(OTHER_LETTER, other));
        }
        return result;
    }

    /**
     * Mọi ngành, xếp theo mã ngành VSIC rồi theo tên (ngành chưa có mã xếp cuối). Dùng cho trang tra cứu cây mã ngành
     * của quản trị: dựng cây và tìm ngay trên trình duyệt (~1.600 dòng, không có mô tả).
     */
    public List<PublicCategoryDTO> allByCode() {
        return loadAll().stream().sorted(BY_CODE).toList();
    }

    /**
     * Ngành nghề lọc theo chữ cái đầu và/hoặc từ khóa.
     *
     * @param letter    một chữ cái (có dấu hay không đều được, Đ = D) hoặc "#"; bỏ trống = mọi chữ. Sai thì 400.
     * @param query     từ khóa trong tên ngành, không phân biệt dấu, hoa thường; nhiều từ thì phải có đủ. Gõ mã ngành
     *                  (vd 4632, 463, G) thì ra ngành có mã bắt đầu bằng mã đó.
     * @param hideEmpty bỏ ngành chưa có doanh nghiệp.
     * @param pageable  sort nhận name (mặc định, A–Z tiếng Việt), listingCount và code; size tối đa {@value #MAX_PAGE_SIZE}.
     */
    public PublicPageDTO<PublicCategoryDTO> categories(String letter, String query, boolean hideEmpty, Pageable pageable) {
        return categories(letter, query, hideEmpty, null, null, pageable);
    }

    /**
     * Như {@link #categories(String, String, boolean, Pageable)}, thêm:
     *
     * @param minCount chỉ lấy ngành có ít nhất chừng này doanh nghiệp (FE cũ dùng &gt; 20 cho trang ngành nghề).
     * @param parent   "root": chỉ ngành gốc; slug hoặc id: chỉ ngành con trực tiếp của ngành đó (404 nếu không có).
     */
    public PublicPageDTO<PublicCategoryDTO> categories(
        String letter,
        String query,
        boolean hideEmpty,
        Long minCount,
        String parent,
        Pageable pageable
    ) {
        List<PublicCategoryDTO> all = loadAll();
        long min = Math.max(hideEmpty ? 1 : 0, minCount == null ? 0 : minCount);
        return filter(all, letter, query, min, parentFilter(all, parent), pageable);
    }

    static PublicPageDTO<PublicCategoryDTO> filter(
        List<PublicCategoryDTO> all,
        String letter,
        String query,
        boolean hideEmpty,
        Pageable pageable
    ) {
        return filter(all, letter, query, hideEmpty ? 1 : 0, c -> true, pageable);
    }

    static PublicPageDTO<PublicCategoryDTO> filter(
        List<PublicCategoryDTO> all,
        String letter,
        String query,
        long minCount,
        Predicate<PublicCategoryDTO> parentFilter,
        Pageable pageable
    ) {
        String wantedLetter = StringUtils.hasText(letter) ? checkedLetter(letter) : null;
        List<String> words = VietnameseText.words(StringUtils.hasText(query) ? fold(query) : "");
        String codePrefix = codePrefix(query);
        Comparator<PublicCategoryDTO> order = comparator(pageable.getSort());
        List<PublicCategoryDTO> matched = all
            .stream()
            .filter(c -> c.listingCount() >= minCount)
            .filter(parentFilter)
            .filter(c -> wantedLetter == null || wantedLetter.equals(c.letter()))
            .filter(
                c ->
                    words.isEmpty() ||
                    containsAll(fold(c.name()), words) ||
                    (codePrefix != null && c.code() != null && c.code().startsWith(codePrefix))
            )
            .sorted(order)
            .toList();
        int size = Math.clamp(pageable.getPageSize(), 1, MAX_PAGE_SIZE);
        int page = Math.max(pageable.getPageNumber(), 0);
        int from = (int) Math.min((long) page * size, matched.size());
        int to = Math.min(from + size, matched.size());
        int totalPages = (matched.size() + size - 1) / size;
        return new PublicPageDTO<>(matched.subList(from, to), page, size, matched.size(), totalPages);
    }

    /** Từ khóa trông như mã ngành (một chữ cái, hoặc 2–5 chữ số) thì trả mã viết hoa, không thì null. */
    static String codePrefix(String query) {
        if (!StringUtils.hasText(query)) {
            return null;
        }
        String trimmed = query.trim().toUpperCase(Locale.ROOT);
        return trimmed.matches("[A-Z]|\\d{1,5}") ? trimmed : null;
    }

    /** "root" → ngành gốc; slug/id → con trực tiếp của ngành đó (không có thì 404); trống → mọi ngành. */
    static Predicate<PublicCategoryDTO> parentFilter(List<PublicCategoryDTO> all, String parent) {
        if (!StringUtils.hasText(parent)) {
            return c -> true;
        }
        if (ROOT.equalsIgnoreCase(parent.trim())) {
            return c -> c.parentId() == null;
        }
        Long parentId = find(all, parent)
            .map(PublicCategoryDTO::id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không có ngành nghề " + parent));
        return c -> parentId.equals(c.parentId());
    }

    /**
     * Chi tiết một ngành theo slug hoặc id: mã, cấp, mô tả, breadcrumb các ngành cha, ngành con trực tiếp (sắp theo mã,
     * ngành chưa có mã theo tên).
     */
    public Optional<PublicTermDetailDTO> category(String slugOrId) {
        List<PublicCategoryDTO> all = loadAll();
        return find(all, slugOrId).map(c -> detail(all, c, categoryPublicRepository.findDescription(c.id())));
    }

    /**
     * Slug ngành cũ (trước khi chuyển sang VSIC 2025) → nơi chuyển hướng 301: slug ngành mới, hoặc null khi ngành cũ không
     * có ngành tương ứng (FE chuyển về trang mục lục). Không phải slug cũ → rỗng.
     */
    public Optional<PublicCategoryDTO.Redirect> redirect(String oldSlug) {
        if (!StringUtils.hasText(oldSlug)) {
            return Optional.empty();
        }
        List<Object> rows = categoryPublicRepository.findRedirectSlug(oldSlug.trim());
        return rows.isEmpty() ? Optional.empty() : Optional.of(new PublicCategoryDTO.Redirect(oldSlug.trim(), (String) rows.getFirst()));
    }

    static PublicTermDetailDTO detail(List<PublicCategoryDTO> all, PublicCategoryDTO category, String description) {
        Map<Long, PublicCategoryDTO> byId = all.stream().collect(Collectors.toMap(PublicCategoryDTO::id, Function.identity()));
        List<PublicTermDetailDTO.Ref> ancestors = new ArrayList<>();
        Set<Long> seen = new HashSet<>(Set.of(category.id()));
        PublicCategoryDTO parent = category.parentId() == null ? null : byId.get(category.parentId());
        // Cây lỗi có vòng lặp cũng không treo
        while (parent != null && seen.add(parent.id())) {
            ancestors.addFirst(ref(parent));
            parent = parent.parentId() == null ? null : byId.get(parent.parentId());
        }
        List<PublicTermDetailDTO.Ref> children = all
            .stream()
            .filter(c -> category.id().equals(c.parentId()))
            .sorted(BY_CODE)
            .map(PublicCategoryService::ref)
            .toList();
        return new PublicTermDetailDTO(
            category.id(),
            category.name(),
            category.slug(),
            StringUtils.hasText(description) ? description.trim() : null,
            null,
            null,
            category.listingCount(),
            List.copyOf(ancestors),
            children,
            category.code(),
            category.level()
        );
    }

    /** Tìm theo slug trước (đúng nguyên văn), không có thì theo id. */
    static Optional<PublicCategoryDTO> find(List<PublicCategoryDTO> all, String slugOrId) {
        if (!StringUtils.hasText(slugOrId)) {
            return Optional.empty();
        }
        String key = slugOrId.trim();
        Optional<PublicCategoryDTO> bySlug = all
            .stream()
            .filter(c -> key.equals(c.slug()))
            .findFirst();
        if (bySlug.isPresent() || !key.chars().allMatch(Character::isDigit) || key.length() > 18) {
            return bySlug;
        }
        long id = Long.parseLong(key);
        return all
            .stream()
            .filter(c -> c.id() == id)
            .findFirst();
    }

    private static PublicTermDetailDTO.Ref ref(PublicCategoryDTO c) {
        return new PublicTermDetailDTO.Ref(c.id(), c.name(), c.slug(), c.listingCount(), c.code(), c.level());
    }

    /** "l", "L", "Đ" → chữ in hoa bỏ dấu; "#" giữ nguyên; còn lại 400. */
    static String checkedLetter(String letter) {
        String trimmed = letter.trim();
        if (OTHER_LETTER.equals(trimmed)) {
            return OTHER_LETTER;
        }
        String folded = letterOf(trimmed);
        if (trimmed.codePointCount(0, trimmed.length()) != 1 || OTHER_LETTER.equals(folded)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "letter phải là một chữ cái A–Z hoặc #, nhận được: " + letter);
        }
        return folded;
    }

    private static Comparator<PublicCategoryDTO> comparator(Sort sort) {
        Comparator<PublicCategoryDTO> result = null;
        for (Sort.Order order : sort) {
            Comparator<PublicCategoryDTO> one = SORTS.get(order.getProperty());
            if (one == null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Không sắp xếp được theo '" + order.getProperty() + "'. Dùng: " + SORTS.keySet()
                );
            }
            one = order.isDescending() ? one.reversed() : one;
            result = result == null ? one : result.thenComparing(one);
        }
        return result == null ? BY_NAME : result;
    }

    List<PublicCategoryDTO> loadAll() {
        return categoryPublicRepository.findAllRows().stream().map(PublicCategoryService::toDto).toList();
    }

    static PublicCategoryDTO toDto(Object[] row) {
        String name = cleanName((String) row[1]);
        Number listingCount = (Number) row[5];
        return new PublicCategoryDTO(
            (Long) row[0],
            row.length > 6 ? (String) row[6] : null,
            name,
            (String) row[2],
            letterOf(name),
            row.length > 7 ? (Integer) row[7] : null,
            (Long) row[3],
            cleanName((String) row[4]),
            listingCount == null ? 0 : listingCount.longValue()
        );
    }

    /** Giải mã {@code &amp;}..., bỏ khoảng trắng thừa. */
    static String cleanName(String name) {
        return name == null ? null : HtmlUtils.htmlUnescape(name).trim();
    }

    /** Chữ cái đầu, in hoa, bỏ dấu (Ă, Â → A; Đ → D; Ô, Ơ → O; Ư → U); không phải chữ cái A–Z thì "#". */
    static String letterOf(String name) {
        if (!StringUtils.hasText(name)) {
            return OTHER_LETTER;
        }
        String trimmed = name.trim();
        String first = new String(Character.toChars(trimmed.codePointAt(0)));
        String folded = VietnameseText.fold(first).toUpperCase(Locale.ROOT);
        return folded.length() == 1 && folded.charAt(0) >= 'A' && folded.charAt(0) <= 'Z' ? folded : OTHER_LETTER;
    }

    private static String fold(String text) {
        return VietnameseText.fold(VietnameseText.normalize(text)).toLowerCase(Locale.ROOT);
    }

    private static boolean containsAll(String foldedName, List<String> words) {
        return words.stream().allMatch(foldedName::contains);
    }
}
