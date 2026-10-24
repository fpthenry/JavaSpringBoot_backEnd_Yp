package com.mycompany.myapp.service;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.mycompany.myapp.domain.Category;
import com.mycompany.myapp.domain.Listing;
import com.mycompany.myapp.domain.Location;
import com.mycompany.myapp.repository.ListingRepository;
import com.mycompany.myapp.repository.search.VietnameseText;
import com.mycompany.myapp.service.dto.publicapi.PublicCategoryDTO;
import com.mycompany.myapp.service.dto.publicapi.PublicListingDTO;
import com.mycompany.myapp.service.dto.publicapi.PublicPageDTO;
import com.mycompany.myapp.service.dvhc.AdministrativeUnits;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilterBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * API công khai doanh nghiệp cho FE (Next.js), thay cho Elasticsearch của WordPress.
 * <ul>
 *   <li>Chỉ doanh nghiệp đã xuất bản ({@code status = publish}).</li>
 *   <li>Tìm, lọc, sắp xếp trên Elasticsearch (index listing); ES chỉ trả id, doanh nghiệp đọc lại từ MySQL kèm ngành nghề,
 *   địa phương, giữ thứ tự của ES (như trang quản trị).</li>
 *   <li>Lọc theo ngành nghề, địa phương bằng slug hoặc id, lấy cả cấp con ({@code categoryIds}, {@code locationIds} đã gồm
 *   các cấp cha).</li>
 *   <li>Từ khóa: mỗi chữ phải có trong ít nhất một field (tên, tên khác, địa chỉ cũ và mới, điện thoại, email, website, mã số
 *   thuế, người đại diện, giới thiệu); tên được ưu tiên. Không phân biệt dấu (cần index listing đã reindex sau khi thêm
 *   field {@code .folded}; index cũ thì chữ không dấu chỉ khớp dữ liệu không dấu).</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class PublicListingService {

    static final String PUBLISHED = "publish";
    static final int MAX_PAGE_SIZE = 50;
    static final int MAX_SUGGESTIONS = 20;
    static final int MAX_RELATED = 30;
    static final int EXCERPT_LENGTH = 300;
    /** sort nhận được → field trên ES. */
    static final Map<String, String> SORT_FIELDS = Map.of(
        "publishedAt",
        "publishedAt",
        "modifiedAt",
        "modifiedAt",
        "viewCount",
        "viewCount",
        "name",
        "name.keyword",
        "id",
        "sortId"
    );
    /**
     * Field tìm từ khóa và độ ưu tiên. Field gốc giữ dấu, {@code .folded} bỏ dấu: chữ có dấu hay không đều khớp
     * ("cong ty" ra "Công ty"), nhưng khớp đúng dấu được cộng điểm ở field gốc nên lên trước.
     * {@code slug} là tên viết không dấu ("cong-ty-tnhh-thang-long-…"): như tìm kiếm trang quản trị, chữ không dấu khớp tên
     * có dấu ngay cả khi index chưa có {@code .folded} (chưa reindex).
     */
    static final List<String> SEARCH_FIELDS = List.of(
        "name^10",
        "name.folded^6",
        "slug^4",
        "nameAlias^5",
        "nameAlias.folded^3",
        "nameEn^3",
        "nameEn.folded^2",
        "address^2",
        "address.folded",
        "addressNew^2",
        "addressNew.folded",
        "phone",
        "mobile",
        "email",
        "website",
        "taxCode^3",
        "representative",
        "representative.folded",
        "description",
        "description.folded^0.5"
    );

    private static final Pattern HTML_TAG = Pattern.compile("<[^>]*>");
    private static final Pattern COORDINATE = Pattern.compile("\"coordinate\"\\s*:\\s*\"([^\"]*)\"");
    private static final Comparator<String> VIETNAMESE = Comparator.nullsLast(Collator.getInstance(Locale.forLanguageTag("vi")));

    private final ElasticsearchTemplate elasticsearchTemplate;
    private final ListingRepository listingRepository;
    private final PublicCategoryService categoryService;
    private final PublicLocationService locationService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public PublicListingService(
        ElasticsearchTemplate elasticsearchTemplate,
        ListingRepository listingRepository,
        PublicCategoryService categoryService,
        PublicLocationService locationService,
        DataSource dataSource,
        ObjectMapper objectMapper
    ) {
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.listingRepository = listingRepository;
        this.categoryService = categoryService;
        this.locationService = locationService;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.objectMapper = objectMapper;
    }

    /**
     * Danh sách doanh nghiệp đã xuất bản.
     *
     * @param query    từ khóa; có từ khóa mà không có sort thì theo độ liên quan.
     * @param category slug hoặc id ngành nghề (lấy cả ngành con); không có ngành đó thì 404.
     * @param location slug (cả slug cũ của website WordPress) hoặc id địa phương (lấy cả đơn vị con); không có thì 404.
     * @param featured true: chỉ doanh nghiệp nổi bật.
     * @param pageable sort nhận {@link #SORT_FIELDS}; không có sort và không có từ khóa: mới đăng trước. size tối đa
     *                 {@value #MAX_PAGE_SIZE}.
     */
    public PublicPageDTO<PublicListingDTO> listings(String query, String category, String location, Boolean featured, Pageable pageable) {
        Long categoryId = StringUtils.hasText(category) ? categoryId(category) : null;
        Long locationId = StringUtils.hasText(location) ? locationId(location) : null;
        Query esQuery = Query.of(q ->
            q.bool(b -> {
                published(b);
                List<String> words = VietnameseText.words(StringUtils.hasText(query) ? VietnameseText.normalize(query.trim()) : "");
                // Mỗi chữ phải có ở ít nhất một field; cả cụm khớp liền trong tên thì lên trước
                for (String word : words) {
                    b.must(m -> m.multiMatch(mm -> mm.query(word).fields(SEARCH_FIELDS).type(TextQueryType.BestFields).tieBreaker(0.3)));
                }
                if (words.size() > 1) {
                    String phrase = String.join(" ", words);
                    b.should(s -> s.matchPhrase(mp -> mp.field("name.folded").query(phrase).boost(5f)));
                    // slug không dấu: "cong ty thang long" khớp "cong-ty-tnhh-thang-long" (cho xen 1 từ như "tnhh";
                    // 2 thì "cong-ty-tnhh-thang-may-long-khang" cũng khớp)
                    b.should(s -> s.matchPhrase(mp -> mp.field("slug").query(VietnameseText.fold(phrase)).slop(1).boost(8f)));
                }
                if (categoryId != null) {
                    b.filter(f -> f.term(t -> t.field("categoryIds").value(categoryId)));
                }
                if (locationId != null) {
                    b.filter(f -> f.term(t -> t.field("locationIds").value(locationId)));
                }
                if (Boolean.TRUE.equals(featured)) {
                    b.filter(f -> f.term(t -> t.field("isFeatured").value(true)));
                }
                return b;
            })
        );
        int size = Math.clamp(pageable.getPageSize(), 1, MAX_PAGE_SIZE);
        int page = Math.max(pageable.getPageNumber(), 0);
        Sort sort = esSort(pageable.getSort(), !VietnameseText.words(query == null ? "" : query).isEmpty());
        NativeQuery nativeQuery = NativeQuery.builder()
            .withQuery(esQuery)
            .withSourceFilter(new FetchSourceFilterBuilder().withIncludes("id").build())
            .withPageable(PageRequest.of(page, size, sort))
            .withTrackTotalHits(true)
            .build();
        SearchHits<Listing> hits = elasticsearchTemplate.search(nativeQuery, Listing.class);
        List<Long> ids = hits
            .stream()
            .map(hit -> Long.valueOf(hit.getId()))
            .toList();
        List<PublicListingDTO> items = loadInOrder(ids)
            .stream()
            .map(l -> toDto(l, false))
            .toList();
        long total = hits.getTotalHits();
        return new PublicPageDTO<>(items, page, size, total, (int) ((total + size - 1) / size));
    }

    /** Chi tiết theo slug (tìm trên ES, đọc từ MySQL). */
    public Optional<PublicListingDTO> listing(String slug) {
        return idBySlug(slug)
            .flatMap(this::loadPublished)
            .map(l -> toDto(l, true));
    }

    /** Chi tiết theo id bài WordPress cũ (URL cũ dạng {@code ?p=913645}). */
    public Optional<PublicListingDTO> listingByWpId(long wpId) {
        return jdbcTemplate
            .queryForList("select id from listing where wp_id = ?", Long.class, wpId)
            .stream()
            .findFirst()
            .flatMap(this::loadPublished)
            .map(l -> toDto(l, true));
    }

    /**
     * Doanh nghiệp cùng ngành (ngành gắn trực tiếp), cùng tỉnh lên trước, rồi nổi bật, rồi mới đăng; không gồm chính nó.
     * Không có doanh nghiệp đó thì empty (404).
     */
    public Optional<List<PublicListingDTO>> related(String slug, int size) {
        Optional<Listing> listing = idBySlug(slug).flatMap(this::loadPublished);
        if (listing.isEmpty()) {
            return Optional.empty();
        }
        List<Long> categoryIds = ids(listing.orElseThrow().getCategories(), Category::getId);
        if (categoryIds.isEmpty()) {
            return Optional.of(List.of());
        }
        Long self = listing.orElseThrow().getId();
        List<Long> provinceIds = listing
            .orElseThrow()
            .getLocations()
            .stream()
            .filter(l -> AdministrativeUnits.PROVINCE.equals(l.getType()))
            .map(Location::getId)
            .toList();
        List<FieldValue> categories = categoryIds.stream().map(FieldValue::of).toList();
        List<FieldValue> provinces = provinceIds.stream().map(FieldValue::of).toList();
        Query esQuery = Query.of(q ->
            q.bool(b -> {
                published(b);
                b.filter(f -> f.terms(t -> t.field("categoryIds").terms(v -> v.value(categories))));
                b.mustNot(n -> n.term(t -> t.field("sortId").value(self)));
                if (!provinces.isEmpty()) {
                    b.should(s -> s.terms(t -> t.field("locationIds").terms(v -> v.value(provinces))));
                }
                b.should(s -> s.term(t -> t.field("isFeatured").value(true).boost(0.5f)));
                return b;
            })
        );
        NativeQuery nativeQuery = NativeQuery.builder()
            .withQuery(esQuery)
            .withSourceFilter(new FetchSourceFilterBuilder().withIncludes("id").build())
            .withPageable(
                PageRequest.of(0, Math.clamp(size, 1, MAX_RELATED), Sort.by(Sort.Order.desc("_score"), Sort.Order.desc("publishedAt")))
            )
            .build();
        List<Long> ids = elasticsearchTemplate
            .search(nativeQuery, Listing.class)
            .stream()
            .map(hit -> Long.valueOf(hit.getId()))
            .toList();
        return Optional.of(
            loadInOrder(ids)
                .stream()
                .map(l -> toDto(l, false))
                .toList()
        );
    }

    /** Gợi ý khi gõ: tên khớp các chữ đã gõ, chữ cuối khớp đầu từ ("cong ty bia ph" → "... Bia Phàn ..."). Chỉ đọc ES. */
    public List<PublicListingDTO.Suggestion> suggest(String query, int size) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        Query esQuery = Query.of(q ->
            q.bool(b -> {
                published(b);
                b.must(m ->
                    m.multiMatch(mm ->
                        mm
                            .query(VietnameseText.normalize(query.trim()))
                            .fields("name^3", "name.folded^2", "slug", "nameAlias", "nameAlias.folded", "nameEn", "nameEn.folded")
                            .type(TextQueryType.BoolPrefix)
                            .operator(Operator.And)
                    )
                );
                return b;
            })
        );
        NativeQuery nativeQuery = NativeQuery.builder()
            .withQuery(esQuery)
            .withSourceFilter(new FetchSourceFilterBuilder().withIncludes("id", "name", "slug").build())
            .withPageable(PageRequest.of(0, Math.clamp(size, 1, MAX_SUGGESTIONS)))
            .build();
        return elasticsearchTemplate
            .search(nativeQuery, Listing.class)
            .stream()
            .map(SearchHit::getContent)
            .filter(l -> StringUtils.hasText(l.getSlug()))
            .map(l -> new PublicListingDTO.Suggestion(l.getId(), l.getName(), l.getSlug()))
            .toList();
    }

    // ---------- Truy vấn ----------

    private static void published(BoolQuery.Builder b) {
        b.filter(f -> f.term(t -> t.field("status.keyword").value(PUBLISHED)));
    }

    private Long categoryId(String slugOrId) {
        return PublicCategoryService.find(categoryService.loadAll(), slugOrId)
            .map(PublicCategoryDTO::id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không có ngành nghề " + slugOrId));
    }

    private Long locationId(String slugOrId) {
        return locationService
            .location(slugOrId)
            .map(l -> l.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không có địa phương " + slugOrId));
    }

    /** Slug → id trên ES (MySQL không có chỉ mục theo slug). */
    private Optional<Long> idBySlug(String slug) {
        if (!StringUtils.hasText(slug)) {
            return Optional.empty();
        }
        NativeQuery nativeQuery = NativeQuery.builder()
            .withQuery(q -> q.term(t -> t.field("slug.keyword").value(slug.trim())))
            .withSourceFilter(new FetchSourceFilterBuilder().withIncludes("id").build())
            .withPageable(PageRequest.of(0, 1))
            .build();
        return elasticsearchTemplate
            .search(nativeQuery, Listing.class)
            .stream()
            .map(hit -> Long.valueOf(hit.getId()))
            .findFirst();
    }

    /** Đọc từ MySQL kèm ngành, địa phương; bản nháp coi như không có. */
    private Optional<Listing> loadPublished(Long id) {
        return listingRepository.findOneWithEagerRelationships(id).filter(l -> PUBLISHED.equals(l.getStatus()));
    }

    /** Đọc từ MySQL kèm ngành, địa phương, giữ thứ tự của ES; bản đã xóa khỏi DB (index chưa cập nhật) thì bỏ qua. */
    private List<Listing> loadInOrder(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, Listing> byId = listingRepository
            .fetchBagRelationships(listingRepository.findAllById(ids))
            .stream()
            .collect(Collectors.toMap(Listing::getId, Function.identity()));
        return ids
            .stream()
            .map(byId::get)
            .filter(Objects::nonNull)
            .filter(l -> PUBLISHED.equals(l.getStatus()))
            .toList();
    }

    /** Có sort: theo sort (thêm id cho ổn định); không có: có từ khóa thì độ liên quan, không thì mới đăng trước. */
    static Sort esSort(Sort sort, boolean hasQuery) {
        List<Sort.Order> orders = new ArrayList<>();
        for (Sort.Order order : sort) {
            String field = SORT_FIELDS.get(order.getProperty());
            if (field == null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Không sắp xếp được theo '" + order.getProperty() + "'. Dùng: " + SORT_FIELDS.keySet()
                );
            }
            orders.add(new Sort.Order(order.getDirection(), field));
        }
        if (orders.isEmpty()) {
            orders.add(hasQuery ? Sort.Order.desc("_score") : Sort.Order.desc("publishedAt"));
        }
        orders.add(Sort.Order.desc("sortId"));
        return Sort.by(orders);
    }

    // ---------- Chuyển sang DTO ----------

    PublicListingDTO toDto(Listing l, boolean detail) {
        return new PublicListingDTO(
            l.getId(),
            l.getWpId(),
            l.getName(),
            l.getNameEn(),
            l.getNameAlias(),
            l.getSlug(),
            detail ? l.getDescription() : excerpt(l.getDescription()),
            l.getPhone(),
            l.getMobile(),
            l.getEmail(),
            l.getWebsite(),
            l.getAddress(),
            l.getAddressNew(),
            coordinate(l.getLocationJson()),
            l.getTaxCode(),
            l.getRepresentative(),
            l.getCapital(),
            l.getFoundedYear(),
            l.getBusinessType(),
            l.getBusinessStatus(),
            l.getIndustryCode(),
            l.getManagedBy(),
            imageUrl(l.getThumbnail()),
            detail ? imageUrls(l.getImages(), objectMapper) : List.of(),
            Boolean.TRUE.equals(l.getIsFeatured()),
            l.getViewCount(),
            l.getPublishedAt(),
            l.getModifiedAt(),
            categoryTerms(l.getCategories()),
            locationTerms(l.getLocations())
        );
    }

    /**
     * Đoạn đầu giới thiệu cho danh sách (thẻ doanh nghiệp): chữ thuần (bỏ thẻ HTML, giải mã {@code &amp;}..., gộp khoảng
     * trắng) để không cắt ngang một thẻ; tối đa {@value #EXCERPT_LENGTH} ký tự, cắt ở khoảng trắng, thêm "…".
     */
    static String excerpt(String description) {
        if (!StringUtils.hasText(description)) {
            return null;
        }
        String text = HtmlUtils.htmlUnescape(HTML_TAG.matcher(description).replaceAll(" ")).replaceAll("\\s+", " ").strip();
        if (text.isEmpty()) {
            return null;
        }
        if (text.length() <= EXCERPT_LENGTH) {
            return text;
        }
        int cut = text.lastIndexOf(' ', EXCERPT_LENGTH);
        return text.substring(0, cut > EXCERPT_LENGTH / 2 ? cut : EXCERPT_LENGTH).strip() + "…";
    }

    /** Tọa độ nguyên văn trong location_json ("21.0001:105.698"), không đổi định dạng, không làm tròn. */
    static String coordinate(String locationJson) {
        if (!StringUtils.hasText(locationJson)) {
            return null;
        }
        Matcher m = COORDINATE.matcher(locationJson);
        return m.find() && StringUtils.hasText(m.group(1)) ? m.group(1) : null;
    }

    /** Chỉ nhận URL http(s); dữ liệu cũ là id ảnh WordPress (chỉ có số) thì null. */
    static String imageUrl(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.startsWith("https://") || trimmed.startsWith("http://") ? trimmed : null;
    }

    /**
     * Ảnh thư viện: mảng JSON gồm URL hoặc đối tượng {@code {"image_url": ...}} (dạng của WordPress), hoặc chuỗi cách nhau
     * bởi dấu phẩy. Bỏ phần không phải URL (id ảnh WordPress), bỏ trùng.
     */
    static List<String> imageUrls(String images, ObjectMapper objectMapper) {
        if (!StringUtils.hasText(images)) {
            return List.of();
        }
        Set<String> urls = new LinkedHashSet<>();
        String trimmed = images.trim();
        if (trimmed.startsWith("[")) {
            try {
                for (JsonNode node : objectMapper.readTree(trimmed)) {
                    String url = node.isObject() ? node.path("image_url").asString(null) : node.asString(null);
                    Optional.ofNullable(imageUrl(url)).ifPresent(urls::add);
                }
            } catch (RuntimeException e) {
                // JSON hỏng: coi như không có ảnh
                return List.of();
            }
        } else {
            for (String part : trimmed.split(",")) {
                Optional.ofNullable(imageUrl(part)).ifPresent(urls::add);
            }
        }
        return List.copyOf(urls);
    }

    private static List<PublicListingDTO.Term> categoryTerms(Collection<Category> categories) {
        return categories
            .stream()
            .map(c ->
                new PublicListingDTO.Term(
                    c.getId(),
                    PublicCategoryService.cleanName(c.getName()),
                    c.getSlug(),
                    c.getParent() == null ? null : c.getParent().getId(),
                    null
                )
            )
            .sorted(Comparator.comparing(PublicListingDTO.Term::name, VIETNAMESE))
            .toList();
    }

    /** Tỉnh trước, rồi quận/huyện, rồi phường/xã; cùng cấp theo tên. */
    private static List<PublicListingDTO.Term> locationTerms(Collection<Location> locations) {
        List<String> levels = List.of(AdministrativeUnits.PROVINCE, AdministrativeUnits.DISTRICT, AdministrativeUnits.WARD);
        return locations
            .stream()
            .map(l ->
                new PublicListingDTO.Term(
                    l.getId(),
                    l.getName(),
                    l.getSlug(),
                    l.getParent() == null ? null : l.getParent().getId(),
                    l.getType()
                )
            )
            .sorted(
                Comparator.comparingInt((PublicListingDTO.Term t) ->
                    levels.indexOf(t.type()) < 0 ? levels.size() : levels.indexOf(t.type())
                ).thenComparing(PublicListingDTO.Term::name, VIETNAMESE)
            )
            .toList();
    }

    private static <T> List<Long> ids(Collection<T> items, Function<T, Long> id) {
        return items == null ? List.of() : items.stream().map(id).filter(Objects::nonNull).distinct().toList();
    }
}
