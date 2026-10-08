package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.BlogCategory;
import com.mycompany.myapp.domain.BlogPost;
import com.mycompany.myapp.domain.Tag;
import com.mycompany.myapp.repository.BlogCategoryRepository;
import com.mycompany.myapp.repository.BlogPostRepository;
import com.mycompany.myapp.repository.BlogPublicRepository;
import com.mycompany.myapp.repository.TagRepository;
import com.mycompany.myapp.repository.search.BlogPostSearchFilter;
import com.mycompany.myapp.repository.search.BlogPostSearchRepository;
import com.mycompany.myapp.service.dto.publicapi.PublicBlogCategoryDTO;
import com.mycompany.myapp.service.dto.publicapi.PublicBlogCategoryDetailDTO;
import com.mycompany.myapp.service.dto.publicapi.PublicBlogPostDTO;
import com.mycompany.myapp.service.dto.publicapi.PublicPageDTO;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * API công khai bài viết cho FE (Next.js): cây danh mục, danh sách bài đã xuất bản lọc theo danh mục (gồm danh mục con),
 * thẻ, từ khóa, và chi tiết bài theo slug. Bài nháp không bao giờ được trả ra.
 */
@Service
@Transactional(readOnly = true)
public class PublicBlogService {

    static final String PUBLISHED = BlogPublicRepository.PUBLISHED;
    static final int MAX_PAGE_SIZE = 100;
    /** Độ dài tối đa của tóm tắt chữ thuần. */
    static final int EXCERPT_LENGTH = 300;
    /** Field được phép sắp xếp (field date/số trong ES; field chữ như title không sắp xếp được). */
    static final Set<String> SORTABLE = Set.of("publishedAt", "viewCount", "id");

    private static final Comparator<String> VIETNAMESE = Comparator.nullsLast(Collator.getInstance(Locale.forLanguageTag("vi")));

    /** Một danh mục đọc từ DB, tách khỏi entity để dựng cây và kiểm thử dễ. */
    record CategoryRow(Long id, Long parentId, String name, String slug, String description) {}

    private final BlogCategoryRepository blogCategoryRepository;
    private final TagRepository tagRepository;
    private final BlogPublicRepository blogPublicRepository;
    private final BlogPostRepository blogPostRepository;
    private final BlogPostSearchRepository blogPostSearchRepository;

    public PublicBlogService(
        BlogCategoryRepository blogCategoryRepository,
        TagRepository tagRepository,
        BlogPublicRepository blogPublicRepository,
        BlogPostRepository blogPostRepository,
        BlogPostSearchRepository blogPostSearchRepository
    ) {
        this.blogCategoryRepository = blogCategoryRepository;
        this.tagRepository = tagRepository;
        this.blogPublicRepository = blogPublicRepository;
        this.blogPostRepository = blogPostRepository;
        this.blogPostSearchRepository = blogPostSearchRepository;
    }

    // ---------- Danh mục ----------

    /**
     * Cây danh mục bài viết.
     *
     * @param hideEmpty bỏ danh mục không có bài đã xuất bản nào (tính cả danh mục con).
     */
    public List<PublicBlogCategoryDTO> categoryTree(boolean hideEmpty) {
        return buildTree(loadCategories(), blogPublicRepository.findPublishedPostCategoryPairs(), hideEmpty);
    }

    /** Một danh mục theo slug hoặc id, kèm cây con và breadcrumb. */
    public Optional<PublicBlogCategoryDetailDTO> category(String slugOrId) {
        List<CategoryRow> rows = loadCategories();
        return findRow(rows, slugOrId).map(row -> {
            List<PublicBlogCategoryDTO> tree = buildTree(rows, blogPublicRepository.findPublishedPostCategoryPairs(), false);
            return new PublicBlogCategoryDetailDTO(findNode(tree, row.id()), breadcrumb(rows, row));
        });
    }

    List<CategoryRow> loadCategories() {
        return blogCategoryRepository
            .findAllWithToOneRelationships()
            .stream()
            .map(c ->
                new CategoryRow(
                    c.getId(),
                    c.getParent() == null ? null : c.getParent().getId(),
                    c.getName(),
                    c.getSlug(),
                    c.getDescription()
                )
            )
            .toList();
    }

    /**
     * Dựng cây từ danh sách phẳng. Con sắp theo tên tiếng Việt. Danh mục có cha không tồn tại hoặc nằm trong vòng lặp
     * (dữ liệu lỗi) được đưa lên gốc để không bị mất.
     *
     * @param postCategoryPairs cặp [id bài, id danh mục] của bài đã xuất bản.
     */
    static List<PublicBlogCategoryDTO> buildTree(List<CategoryRow> rows, List<Object[]> postCategoryPairs, boolean hideEmpty) {
        Map<Long, CategoryRow> byId = new HashMap<>();
        rows.forEach(row -> byId.put(row.id(), row));
        Map<Long, Set<Long>> directPosts = new HashMap<>();
        for (Object[] pair : postCategoryPairs) {
            directPosts.computeIfAbsent((Long) pair[1], id -> new HashSet<>()).add((Long) pair[0]);
        }
        Map<Long, List<CategoryRow>> children = new HashMap<>();
        List<CategoryRow> roots = new ArrayList<>();
        for (CategoryRow row : rows) {
            if (row.parentId() == null || !byId.containsKey(row.parentId()) || inCycle(row, byId)) {
                roots.add(row);
            } else {
                children.computeIfAbsent(row.parentId(), id -> new ArrayList<>()).add(row);
            }
        }
        List<PublicBlogCategoryDTO> result = new ArrayList<>();
        for (CategoryRow root : sortByName(roots)) {
            Node node = build(root, children, directPosts, hideEmpty);
            if (!hideEmpty || node.dto().totalPostCount() > 0) {
                result.add(node.dto());
            }
        }
        return result;
    }

    /** Nút đang dựng: DTO và tập id bài của cả cây con (để đếm mỗi bài một lần). */
    private record Node(PublicBlogCategoryDTO dto, Set<Long> subtreePosts) {}

    private static Node build(CategoryRow row, Map<Long, List<CategoryRow>> children, Map<Long, Set<Long>> directPosts, boolean hideEmpty) {
        Set<Long> direct = directPosts.getOrDefault(row.id(), Set.of());
        Set<Long> subtree = new HashSet<>(direct);
        List<PublicBlogCategoryDTO> childDtos = new ArrayList<>();
        for (CategoryRow child : sortByName(children.getOrDefault(row.id(), List.of()))) {
            Node node = build(child, children, directPosts, hideEmpty);
            subtree.addAll(node.subtreePosts());
            if (!hideEmpty || node.dto().totalPostCount() > 0) {
                childDtos.add(node.dto());
            }
        }
        PublicBlogCategoryDTO dto = new PublicBlogCategoryDTO(
            row.id(),
            row.name(),
            row.slug(),
            row.description(),
            row.parentId(),
            direct.size(),
            subtree.size(),
            List.copyOf(childDtos)
        );
        return new Node(dto, subtree);
    }

    private static boolean inCycle(CategoryRow row, Map<Long, CategoryRow> byId) {
        Set<Long> seen = new HashSet<>();
        Long id = row.id();
        while (id != null) {
            if (!seen.add(id)) {
                return true;
            }
            CategoryRow current = byId.get(id);
            id = current == null ? null : current.parentId();
        }
        return false;
    }

    private static List<CategoryRow> sortByName(List<CategoryRow> rows) {
        return rows.stream().sorted(Comparator.comparing(CategoryRow::name, VIETNAMESE)).toList();
    }

    private static PublicBlogCategoryDTO findNode(List<PublicBlogCategoryDTO> nodes, Long id) {
        for (PublicBlogCategoryDTO node : nodes) {
            if (node.id().equals(id)) {
                return node;
            }
            PublicBlogCategoryDTO found = findNode(node.children(), id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    static List<PublicBlogPostDTO.Term> breadcrumb(List<CategoryRow> rows, CategoryRow row) {
        Map<Long, CategoryRow> byId = new HashMap<>();
        rows.forEach(r -> byId.put(r.id(), r));
        List<PublicBlogPostDTO.Term> path = new ArrayList<>();
        Set<Long> seen = new HashSet<>(Set.of(row.id()));
        Long parentId = row.parentId();
        while (parentId != null && seen.add(parentId) && byId.containsKey(parentId)) {
            CategoryRow parent = byId.get(parentId);
            path.add(0, new PublicBlogPostDTO.Term(parent.id(), parent.name(), parent.slug()));
            parentId = parent.parentId();
        }
        return path;
    }

    /** Tìm theo slug; chuỗi toàn số thì tìm theo id (FE có thể dùng một trong hai). */
    static Optional<CategoryRow> findRow(List<CategoryRow> rows, String slugOrId) {
        return findBySlugOrId(rows, slugOrId, CategoryRow::slug, CategoryRow::id);
    }

    private static <T> Optional<T> findBySlugOrId(List<T> items, String slugOrId, Function<T, String> slug, Function<T, Long> id) {
        if (!StringUtils.hasText(slugOrId)) {
            return Optional.empty();
        }
        String key = slugOrId.trim();
        Optional<T> bySlug = items
            .stream()
            .filter(item -> key.equalsIgnoreCase(slug.apply(item)))
            .findFirst();
        if (bySlug.isPresent() || !key.chars().allMatch(Character::isDigit)) {
            return bySlug;
        }
        Long wanted = Long.valueOf(key);
        return items
            .stream()
            .filter(item -> wanted.equals(id.apply(item)))
            .findFirst();
    }

    // ---------- Bài viết ----------

    /**
     * Danh sách bài đã xuất bản (tìm trên Elasticsearch, đọc lại từ MySQL).
     *
     * @param category slug hoặc id danh mục; lấy cả bài của danh mục con. Không có danh mục này thì 404.
     * @param tag      slug hoặc id thẻ. Không có thẻ này thì 404.
     * @param query    từ khóa (có dấu khớp đúng dấu, không dấu khớp mọi dấu).
     * @param pageable sort chỉ nhận {@link #SORTABLE}; mặc định bài mới đăng trước (có từ khóa thì theo độ liên quan).
     */
    public PublicPageDTO<PublicBlogPostDTO> posts(String category, String tag, String query, Pageable pageable) {
        Long categoryId = null;
        if (StringUtils.hasText(category)) {
            categoryId = findRow(loadCategories(), category)
                .map(CategoryRow::id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không có danh mục bài viết: " + category));
        }
        Long tagId = null;
        if (StringUtils.hasText(tag)) {
            tagId = findBySlugOrId(tagRepository.findAll(), tag, Tag::getSlug, Tag::getId)
                .map(Tag::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không có thẻ: " + tag));
        }
        Pageable paging = checkedPageable(pageable);
        Page<BlogPost> page = blogPostSearchRepository.search(new BlogPostSearchFilter(query, categoryId, tagId, PUBLISHED), paging);
        List<PublicBlogPostDTO> items = page
            .getContent()
            .stream()
            .filter(post -> PUBLISHED.equals(post.getStatus()))
            .map(post -> toDto(post, false))
            .toList();
        return new PublicPageDTO<>(items, paging.getPageNumber(), paging.getPageSize(), page.getTotalElements(), page.getTotalPages());
    }

    /** Giới hạn cỡ trang 1–{@value #MAX_PAGE_SIZE}; sort ngoài {@link #SORTABLE} thì 400. */
    static Pageable checkedPageable(Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!SORTABLE.contains(order.getProperty())) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Không sắp xếp được theo '" + order.getProperty() + "'. Dùng: " + SORTABLE
                );
            }
        }
        int size = Math.clamp(pageable.getPageSize(), 1, MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(pageable.getPageNumber(), 0), size, pageable.getSort());
    }

    /** Chi tiết bài đã xuất bản theo slug (kèm nội dung đã bỏ shortcode). */
    public Optional<PublicBlogPostDTO> post(String slug) {
        if (!StringUtils.hasText(slug)) {
            return Optional.empty();
        }
        return blogPublicRepository
            .findPublishedBySlug(slug.trim(), PageRequest.of(0, 1))
            .stream()
            .findFirst()
            .flatMap(post -> blogPostRepository.findOneWithEagerRelationships(post.getId()))
            .map(post -> toDto(post, true));
    }

    static PublicBlogPostDTO toDto(BlogPost post, boolean withContent) {
        String excerpt = BlogContent.plainText(post.getExcerpt(), EXCERPT_LENGTH);
        if (!StringUtils.hasText(excerpt)) {
            excerpt = BlogContent.plainText(post.getContent(), EXCERPT_LENGTH);
        }
        return new PublicBlogPostDTO(
            post.getId(),
            post.getTitle(),
            post.getSlug(),
            excerpt,
            withContent ? BlogContent.cleanHtml(post.getContent()) : null,
            post.getThumbnail(),
            post.getAuthorName(),
            post.getViewCount(),
            post.getPublishedAt(),
            post.getUpdatedAt(),
            terms(post.getCategories(), BlogCategory::getId, BlogCategory::getName, BlogCategory::getSlug),
            terms(post.getTags(), Tag::getId, Tag::getName, Tag::getSlug)
        );
    }

    private static <T> List<PublicBlogPostDTO.Term> terms(
        Set<T> items,
        Function<T, Long> id,
        Function<T, String> name,
        Function<T, String> slug
    ) {
        return items
            .stream()
            .filter(Objects::nonNull)
            .map(item -> new PublicBlogPostDTO.Term(id.apply(item), name.apply(item), slug.apply(item)))
            .sorted(Comparator.comparing(PublicBlogPostDTO.Term::name, VIETNAMESE))
            .toList();
    }
}
