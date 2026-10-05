package com.mycompany.myapp.service.mapper;

import static com.mycompany.myapp.domain.BlogCategoryAsserts.*;
import static com.mycompany.myapp.domain.BlogCategoryTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BlogCategoryMapperTest {

    private BlogCategoryMapper blogCategoryMapper;

    @BeforeEach
    void setUp() {
        blogCategoryMapper = new BlogCategoryMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getBlogCategorySample1();
        var actual = blogCategoryMapper.toEntity(blogCategoryMapper.toDto(expected));
        assertBlogCategoryAllPropertiesEquals(expected, actual);
    }
}
