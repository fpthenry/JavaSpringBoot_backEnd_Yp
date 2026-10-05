package com.mycompany.myapp.repository.search;

import com.mycompany.myapp.domain.BlogCategory;
import com.mycompany.myapp.domain.BlogPost;
import com.mycompany.myapp.domain.Tag;
import com.mycompany.myapp.repository.BlogCategoryRepository;
import com.mycompany.myapp.repository.TagRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Điền các field chỉ có trong Elasticsearch của {@link BlogPost} ({@code categoryIds}, {@code categoryNames},
 * {@code tagIds}, {@code tagNames}) trước khi ghi vào index.
 * <p>
 * Chỉ dùng id của danh mục, thẻ gắn trên bài (bài vừa lưu từ DTO chỉ có id); tên và danh mục cha đọc từ DB.
 * {@code categoryIds} gồm cả các danh mục cha, nên lọc theo "Sự kiện" ra cả bài thuộc "Tháng khuyến mại" (con của nó).
 * Gọi trong transaction đang lưu bài, để luồng index chạy nền không phải đọc lại DB (lúc đó có thể chưa commit).
 */
@Component
public class BlogPostSearchFields {

    /** Một danh mục trong cây: id cha (null với danh mục gốc) và tên. */
    record CategoryNode(Long parentId, String name) {}

    private final BlogCategoryRepository blogCategoryRepository;
    private final TagRepository tagRepository;

    public BlogPostSearchFields(BlogCategoryRepository blogCategoryRepository, TagRepository tagRepository) {
        this.blogCategoryRepository = blogCategoryRepository;
        this.tagRepository = tagRepository;
    }

    public BlogPost fill(BlogPost post) {
        fill(List.of(post));
        return post;
    }

    public List<BlogPost> fill(List<BlogPost> posts) {
        Map<Long, CategoryNode> categories = loadCategories();
        Set<Long> tagIds = posts
            .stream()
            .flatMap(post -> post.getTags().stream())
            .map(Tag::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, String> tagNames = new HashMap<>();
        if (!tagIds.isEmpty()) {
            tagRepository.findAllById(tagIds).forEach(tag -> tagNames.put(tag.getId(), tag.getName()));
        }
        posts.forEach(post -> fill(post, categories, tagNames));
        return posts;
    }

    /** Cả cây danh mục bài viết (chỉ vài chục dòng nên đọc lại mỗi lần). */
    Map<Long, CategoryNode> loadCategories() {
        Map<Long, CategoryNode> categories = new HashMap<>();
        for (Object[] row : blogCategoryRepository.findAllTreeRows()) {
            categories.put((Long) row[0], new CategoryNode((Long) row[1], (String) row[2]));
        }
        return categories;
    }

    static void fill(BlogPost post, Map<Long, CategoryNode> categories, Map<Long, String> tagNames) {
        Set<Long> categoryIds = new HashSet<>();
        Set<String> categoryNames = new HashSet<>();
        for (BlogCategory category : post.getCategories()) {
            CategoryNode node = categories.get(category.getId());
            if (node != null && node.name() != null) {
                categoryNames.add(VietnameseText.normalize(node.name()));
            }
            // Đi ngược lên gốc; add() trả false khi đã gặp, nên cây lỗi có vòng lặp cũng không treo
            Long id = category.getId();
            while (id != null && categoryIds.add(id)) {
                CategoryNode current = categories.get(id);
                id = current == null ? null : current.parentId();
            }
        }
        post.setCategoryIds(categoryIds);
        post.setCategoryNames(categoryNames);
        post.setTagIds(post.getTags().stream().map(Tag::getId).filter(Objects::nonNull).collect(Collectors.toSet()));
        post.setTagNames(
            post
                .getTags()
                .stream()
                .map(tag -> tagNames.get(tag.getId()))
                .filter(Objects::nonNull)
                .map(VietnameseText::normalize)
                .collect(Collectors.toSet())
        );
    }
}
