package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.BlogCategoryTestSamples.*;
import static com.mycompany.myapp.domain.BlogCategoryTestSamples.*;
import static com.mycompany.myapp.domain.BlogPostTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BlogCategoryTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(BlogCategory.class);
        BlogCategory blogCategory1 = getBlogCategorySample1();
        BlogCategory blogCategory2 = new BlogCategory();
        assertThat(blogCategory1).isNotEqualTo(blogCategory2);

        blogCategory2.setId(blogCategory1.getId());
        assertThat(blogCategory1).isEqualTo(blogCategory2);

        blogCategory2 = getBlogCategorySample2();
        assertThat(blogCategory1).isNotEqualTo(blogCategory2);
    }

    @Test
    void parentTest() {
        BlogCategory blogCategory = getBlogCategoryRandomSampleGenerator();
        BlogCategory blogCategoryBack = getBlogCategoryRandomSampleGenerator();

        blogCategory.setParent(blogCategoryBack);
        assertThat(blogCategory.getParent()).isEqualTo(blogCategoryBack);

        blogCategory.parent(null);
        assertThat(blogCategory.getParent()).isNull();
    }

    @Test
    void blogPostTest() {
        BlogCategory blogCategory = getBlogCategoryRandomSampleGenerator();
        BlogPost blogPostBack = getBlogPostRandomSampleGenerator();

        blogCategory.addBlogPost(blogPostBack);
        assertThat(blogCategory.getBlogPosts()).containsOnly(blogPostBack);
        assertThat(blogPostBack.getCategories()).containsOnly(blogCategory);

        blogCategory.removeBlogPost(blogPostBack);
        assertThat(blogCategory.getBlogPosts()).doesNotContain(blogPostBack);
        assertThat(blogPostBack.getCategories()).doesNotContain(blogCategory);

        blogCategory.blogPosts(new HashSet<>(Set.of(blogPostBack)));
        assertThat(blogCategory.getBlogPosts()).containsOnly(blogPostBack);
        assertThat(blogPostBack.getCategories()).containsOnly(blogCategory);

        blogCategory.setBlogPosts(new HashSet<>());
        assertThat(blogCategory.getBlogPosts()).doesNotContain(blogPostBack);
        assertThat(blogPostBack.getCategories()).doesNotContain(blogCategory);
    }
}
