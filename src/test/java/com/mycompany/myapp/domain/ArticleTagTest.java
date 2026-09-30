package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.ArticleTagTestSamples.*;
import static com.mycompany.myapp.domain.ArticleTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ArticleTagTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ArticleTag.class);
        ArticleTag articleTag1 = getArticleTagSample1();
        ArticleTag articleTag2 = new ArticleTag();
        assertThat(articleTag1).isNotEqualTo(articleTag2);

        articleTag2.setId(articleTag1.getId());
        assertThat(articleTag1).isEqualTo(articleTag2);

        articleTag2 = getArticleTagSample2();
        assertThat(articleTag1).isNotEqualTo(articleTag2);
    }

    @Test
    void articlesTest() {
        ArticleTag articleTag = getArticleTagRandomSampleGenerator();
        Article articleBack = getArticleRandomSampleGenerator();

        articleTag.addArticles(articleBack);
        assertThat(articleTag.getArticleses()).containsOnly(articleBack);
        assertThat(articleBack.getTagses()).containsOnly(articleTag);

        articleTag.removeArticles(articleBack);
        assertThat(articleTag.getArticleses()).doesNotContain(articleBack);
        assertThat(articleBack.getTagses()).doesNotContain(articleTag);

        articleTag.articleses(new HashSet<>(Set.of(articleBack)));
        assertThat(articleTag.getArticleses()).containsOnly(articleBack);
        assertThat(articleBack.getTagses()).containsOnly(articleTag);

        articleTag.setArticleses(new HashSet<>());
        assertThat(articleTag.getArticleses()).doesNotContain(articleBack);
        assertThat(articleBack.getTagses()).doesNotContain(articleTag);
    }
}
