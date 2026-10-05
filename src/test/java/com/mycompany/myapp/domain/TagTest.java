package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.BlogPostTestSamples.*;
import static com.mycompany.myapp.domain.TagTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TagTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Tag.class);
        Tag tag1 = getTagSample1();
        Tag tag2 = new Tag();
        assertThat(tag1).isNotEqualTo(tag2);

        tag2.setId(tag1.getId());
        assertThat(tag1).isEqualTo(tag2);

        tag2 = getTagSample2();
        assertThat(tag1).isNotEqualTo(tag2);
    }

    @Test
    void blogPostTest() {
        Tag tag = getTagRandomSampleGenerator();
        BlogPost blogPostBack = getBlogPostRandomSampleGenerator();

        tag.addBlogPost(blogPostBack);
        assertThat(tag.getBlogPosts()).containsOnly(blogPostBack);
        assertThat(blogPostBack.getTags()).containsOnly(tag);

        tag.removeBlogPost(blogPostBack);
        assertThat(tag.getBlogPosts()).doesNotContain(blogPostBack);
        assertThat(blogPostBack.getTags()).doesNotContain(tag);

        tag.blogPosts(new HashSet<>(Set.of(blogPostBack)));
        assertThat(tag.getBlogPosts()).containsOnly(blogPostBack);
        assertThat(blogPostBack.getTags()).containsOnly(tag);

        tag.setBlogPosts(new HashSet<>());
        assertThat(tag.getBlogPosts()).doesNotContain(blogPostBack);
        assertThat(blogPostBack.getTags()).doesNotContain(tag);
    }
}
