package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.BlogPostTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
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
}
