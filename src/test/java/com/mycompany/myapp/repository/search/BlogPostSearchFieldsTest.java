package com.mycompany.myapp.repository.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.domain.BlogCategory;
import com.mycompany.myapp.domain.BlogPost;
import com.mycompany.myapp.domain.Tag;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
            .contains("\"query\":\"khuyen\"")
            .contains("\"query\":\"mai\"")
            .doesNotContain("title.exact^3")
            .contains("{\"term\":{\"categoryIds\":{\"value\":2}}}")
            .contains("{\"term\":{\"tagIds\":{\"value\":9}}}")
            .contains("{\"term\":{\"status.keyword\":{\"value\":\"publish\"}}}");
    }

    @Test
    void tuCoDauTimTrenFieldGiuDauTuKhongDauTimTrenFieldBoDau() {
        // "khuyến" có dấu -> .exact; "mai" không dấu -> field bỏ dấu; "hoà" kiểu cũ được đổi thành "hòa"
        String json = BlogPostSearchRepositoryInternalImpl.buildQuery(BlogPostSearchFilter.ofQuery("khuyến mai, hoà")).toString();
        // Mỗi từ là một multi_match riêng; gom theo từ để kiểm tra field, không phụ thuộc thứ tự key trong JSON
        Map<String, String> byWord = new HashMap<>();
        Matcher multiMatch = Pattern.compile("\\{\"multi_match\":\\{[^}]*\\}").matcher(json);
        while (multiMatch.find()) {
            Matcher word = Pattern.compile("\"query\":\"([^\"]*)\"").matcher(multiMatch.group());
            if (word.find()) {
                byWord.put(word.group(1), multiMatch.group());
            }
        }
        assertThat(byWord).containsOnlyKeys("khuyến", "mai", "hòa");
        assertThat(byWord.get("khuyến")).contains("title.exact^3").contains("categoryNames.exact^2");
        assertThat(byWord.get("hòa")).contains("content.exact");
        assertThat(byWord.get("mai")).contains("\"title^3\"").doesNotContain(".exact");
        assertThat(json).contains("\"query\":\"khuyến mai hòa\"");
    }

    @Test
    void queryRongThiLayTatCa() {
        String json = BlogPostSearchRepositoryInternalImpl.buildQuery(BlogPostSearchFilter.ofQuery("  ")).toString();
        assertThat(json).contains("match_all").doesNotContain("multi_match").doesNotContain("filter");
    }
}
