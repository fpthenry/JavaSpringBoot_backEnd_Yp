package com.mycompany.myapp.repository.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.domain.BlogCategory;
import com.mycompany.myapp.domain.BlogPost;
import com.mycompany.myapp.domain.Tag;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BlogPostSearchFieldsTest {

    private static BlogCategory category(long id, String name) {
        BlogCategory category = new BlogCategory();
        category.setId(id);
        category.setName(name);
        return category;
    }

    private static Tag tag(long id, String name) {
        Tag tag = new Tag();
        tag.setId(id);
        tag.setName(name);
        return tag;
    }

    @Test
    void categoryIdsGomCaDanhMucChaVaKhongTreoKhiCayCoVong() {
        // 1 Sự kiện (gốc) <- 3 Tháng khuyến mại <- 7 Hà Nội; 8 <-> 9 trỏ vòng nhau (dữ liệu lỗi)
        Map<Long, BlogPostSearchFields.CategoryNode> categories = new HashMap<>();
        categories.put(1L, new BlogPostSearchFields.CategoryNode(null, "Sự kiện"));
        categories.put(3L, new BlogPostSearchFields.CategoryNode(1L, "Tháng khuyến mại"));
        categories.put(7L, new BlogPostSearchFields.CategoryNode(3L, "Hà Nội"));
        categories.put(8L, new BlogPostSearchFields.CategoryNode(9L, "Vòng"));
        categories.put(9L, new BlogPostSearchFields.CategoryNode(8L, "Vòng 2"));
        // Bài vừa lưu từ DTO: danh mục, thẻ chỉ có id, tên lấy từ DB
        BlogPost post = new BlogPost()
            .addCategory(category(7, null))
            .addCategory(category(8, null))
            .addTag(tag(5, null))
            .addTag(tag(6, null));

        BlogPostSearchFields.fill(post, categories, Map.of(5L, "khuyến mãi"));

        assertThat(post.getCategoryIds()).containsExactlyInAnyOrder(7L, 3L, 1L, 8L, 9L);
        assertThat(post.getCategoryNames()).containsExactlyInAnyOrder("Hà Nội", "Vòng");
        assertThat(post.getTagIds()).containsExactlyInAnyOrder(5L, 6L);
        assertThat(post.getTagNames()).containsExactly("khuyến mãi");
    }

    @Test
    void baiKhongCoDanhMucThiFieldRong() {
        BlogPost post = new BlogPost();
        BlogPostSearchFields.fill(post, Map.of(), Map.of());
        assertThat(post.getCategoryIds()).isEmpty();
        assertThat(post.getTagNames()).isEmpty();
    }

    @Test
    void queryCoTuKhoaVaBoLoc() {
        String json = BlogPostSearchRepositoryInternalImpl.buildQuery(
            new BlogPostSearchFilter(" khuyen mai ", 2L, 9L, "publish")
        ).toString();
        assertThat(json)
            .contains("\"query\":\"khuyen mai\"")
            .contains("\"type\":\"cross_fields\"")
            .contains("\"operator\":\"and\"")
            .contains("title.exact^3")
            .contains("{\"term\":{\"categoryIds\":{\"value\":2}}}")
            .contains("{\"term\":{\"tagIds\":{\"value\":9}}}")
            .contains("{\"term\":{\"status.keyword\":{\"value\":\"publish\"}}}");
    }

    @Test
    void queryRongThiLayTatCa() {
        String json = BlogPostSearchRepositoryInternalImpl.buildQuery(BlogPostSearchFilter.ofQuery("  ")).toString();
        assertThat(json).contains("match_all").doesNotContain("multi_match").doesNotContain("filter");
    }
}
