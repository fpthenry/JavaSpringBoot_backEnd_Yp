package com.mycompany.myapp.service.mapper;

import static com.mycompany.myapp.domain.ArticleAsserts.*;
import static com.mycompany.myapp.domain.ArticleTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ArticleMapperTest {

    private ArticleMapper articleMapper;

    @BeforeEach
    void setUp() {
        articleMapper = new ArticleMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getArticleSample1();
        var actual = articleMapper.toEntity(articleMapper.toDto(expected));
        assertArticleAllPropertiesEquals(expected, actual);
    }
}
