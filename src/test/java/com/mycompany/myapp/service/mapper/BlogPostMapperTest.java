package com.mycompany.myapp.service.mapper;

import static com.mycompany.myapp.domain.BlogPostAsserts.*;
import static com.mycompany.myapp.domain.BlogPostTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BlogPostMapperTest {

    private BlogPostMapper blogPostMapper;

    @BeforeEach
    void setUp() {
        blogPostMapper = new BlogPostMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getBlogPostSample1();
        var actual = blogPostMapper.toEntity(blogPostMapper.toDto(expected));
        assertBlogPostAllPropertiesEquals(expected, actual);
    }
}
