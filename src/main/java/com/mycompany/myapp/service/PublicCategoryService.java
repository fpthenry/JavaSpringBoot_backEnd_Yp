package com.mycompany.myapp.service;

import com.mycompany.myapp.repository.CategoryPublicRepository;
import com.mycompany.myapp.repository.search.VietnameseText;
import com.mycompany.myapp.service.dto.publicapi.PublicCategoryDTO;
import com.mycompany.myapp.service.dto.publicapi.PublicPageDTO;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
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
    static final int MAX_PAGE_SIZE = 500;
    static final Map<String, Comparator<PublicCategoryDTO>> SORTS;

    private static final Comparator<String> VIETNAMESE = Comparator.nullsLast(Collator.getInstance(Locale.forLanguageTag("vi")));
    private static final Comparator<PublicCategoryDTO> BY_NAME = Comparator.comparing(PublicCategoryDTO::name, VIETNAMESE).thenComparing(
        PublicCategoryDTO::id
    );

    static {
        SORTS = Map.of("name", BY_NAME, "listingCount", Comparator.comparingLong(PublicCategoryDTO::listingCount).thenComparing(BY_NAME));
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
     * Ngành nghề lọc theo chữ cái đầu và/hoặc từ khóa.
     *
     * @param letter    một chữ cái (có dấu hay không đều được, Đ = D) hoặc "#"; bỏ trống = mọi chữ. Sai thì 400.
     * @param query     từ khóa trong tên ngành, không phân biệt dấu, hoa thường; nhiều từ thì phải có đủ.
     * @param hideEmpty bỏ ngành chưa có doanh nghiệp.
     * @param pageable  sort nhận name (mặc định, A–Z tiếng Việt) và listingCount; size tối đa {@value #MAX_PAGE_SIZE}.
     */
    public PublicPageDTO<PublicCategoryDTO> categories(String letter, String query, boolean hideEmpty, Pageable pageable) {
        return filter(loadAll(), letter, query, hideEmpty, pageable);
    }

    static PublicPageDTO<PublicCategoryDTO> filter(
        List<PublicCategoryDTO> all,
        String letter,
        String query,
        boolean hideEmpty,
        Pageable pageable
    ) {
        String wantedLetter = StringUtils.hasText(letter) ? checkedLetter(letter) : null;
        List<String> words = VietnameseText.words(StringUtils.hasText(query) ? fold(query) : "");
        Comparator<PublicCategoryDTO> order = comparator(pageable.getSort());
        List<PublicCategoryDTO> matched = all
            .stream()
            .filter(c -> !hideEmpty || c.listingCount() > 0)
            .filter(c -> wantedLetter == null || wantedLetter.equals(c.letter()))
            .filter(c -> words.isEmpty() || containsAll(fold(c.name()), words))
            .sorted(order)
            .toList();
        int size = Math.clamp(pageable.getPageSize(), 1, MAX_PAGE_SIZE);
        int page = Math.max(pageable.getPageNumber(), 0);
        int from = (int) Math.min((long) page * size, matched.size());
        int to = Math.min(from + size, matched.size());
        int totalPages = (matched.size() + size - 1) / size;
        return new PublicPageDTO<>(matched.subList(from, to), page, size, matched.size(), totalPages);
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
            name,
            (String) row[2],
            letterOf(name),
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
