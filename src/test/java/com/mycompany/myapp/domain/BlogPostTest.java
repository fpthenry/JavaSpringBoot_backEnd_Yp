package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.BlogCategoryTestSamples.*;
import static com.mycompany.myapp.domain.BlogPostTestSamples.*;
import static com.mycompany.myapp.domain.TagTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BlogPostTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(BlogPost.class);
        BlogPost blogPost1 = getBlogPostSample1();
        BlogPost blogPost2 = new BlogPost();
        assertThat(blogPost1).isNotEqualTo(blogPost2);

        blogPost2.setId(blogPost1.getId());
        assertThat(blogPost1).isEqualTo(blogPost2);

        blogPost2 = getBlogPostSample2();
        assertThat(blogPost1).isNotEqualTo(blogPost2);
    }

    @Test
    void categoryTest() {
        BlogPost blogPost = getBlogPostRandomSampleGenerator();
        BlogCategory blogCategoryBack = getBlogCategoryRandomSampleGenerator();

        blogPost.addCategory(blogCategoryBack);
        assertThat(blogPost.getCategories()).containsOnly(blogCategoryBack);

        blogPost.removeCategory(blogCategoryBack);
        assertThat(blogPost.getCategories()).doesNotContain(blogCategoryBack);

        blogPost.categories(new HashSet<>(Set.of(blogCategoryBack)));
        assertThat(blogPost.getCategories()).containsOnly(blogCategoryBack);

        blogPost.setCategories(new HashSet<>());
        assertThat(blogPost.getCategories()).doesNotContain(blogCategoryBack);
    }

    @Test
    void tagTest() {
        BlogPost blogPost = getBlogPostRandomSampleGenerator();
        Tag tagBack = getTagRandomSampleGenerator();

        blogPost.addTag(tagBack);
        assertThat(blogPost.getTags()).containsOnly(tagBack);

        blogPost.removeTag(tagBack);
        assertThat(blogPost.getTags()).doesNotContain(tagBack);

        blogPost.tags(new HashSet<>(Set.of(tagBack)));
        assertThat(blogPost.getTags()).containsOnly(tagBack);

        blogPost.setTags(new HashSet<>());
        assertThat(blogPost.getTags()).doesNotContain(tagBack);
    }
}
