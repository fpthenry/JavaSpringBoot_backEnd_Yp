package com.mycompany.myapp.service.mapper;

import static com.mycompany.myapp.domain.ArticleTagAsserts.*;
import static com.mycompany.myapp.domain.ArticleTagTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ArticleTagMapperTest {

    private ArticleTagMapper articleTagMapper;

    @BeforeEach
    void setUp() {
        articleTagMapper = new ArticleTagMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getArticleTagSample1();
        var actual = articleTagMapper.toEntity(articleTagMapper.toDto(expected));
        assertArticleTagAllPropertiesEquals(expected, actual);
    }
}
