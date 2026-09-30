package com.mycompany.myapp.service.mapper;

import static com.mycompany.myapp.domain.ArticleCategoryAsserts.*;
import static com.mycompany.myapp.domain.ArticleCategoryTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ArticleCategoryMapperTest {

    private ArticleCategoryMapper articleCategoryMapper;

    @BeforeEach
    void setUp() {
        articleCategoryMapper = new ArticleCategoryMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getArticleCategorySample1();
        var actual = articleCategoryMapper.toEntity(articleCategoryMapper.toDto(expected));
        assertArticleCategoryAllPropertiesEquals(expected, actual);
    }
}
