package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.ArticleCategoryTestSamples.*;
import static com.mycompany.myapp.domain.ArticleTagTestSamples.*;
import static com.mycompany.myapp.domain.ArticleTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ArticleTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Article.class);
        Article article1 = getArticleSample1();
        Article article2 = new Article();
        assertThat(article1).isNotEqualTo(article2);

        article2.setId(article1.getId());
        assertThat(article1).isEqualTo(article2);

        article2 = getArticleSample2();
        assertThat(article1).isNotEqualTo(article2);
    }

    @Test
    void categoriesTest() {
        Article article = getArticleRandomSampleGenerator();
        ArticleCategory articleCategoryBack = getArticleCategoryRandomSampleGenerator();

        article.addCategories(articleCategoryBack);
        assertThat(article.getCategorieses()).containsOnly(articleCategoryBack);

        article.removeCategories(articleCategoryBack);
        assertThat(article.getCategorieses()).doesNotContain(articleCategoryBack);

        article.categorieses(new HashSet<>(Set.of(articleCategoryBack)));
        assertThat(article.getCategorieses()).containsOnly(articleCategoryBack);

        article.setCategorieses(new HashSet<>());
        assertThat(article.getCategorieses()).doesNotContain(articleCategoryBack);
    }

    @Test
    void tagsTest() {
        Article article = getArticleRandomSampleGenerator();
        ArticleTag articleTagBack = getArticleTagRandomSampleGenerator();

        article.addTags(articleTagBack);
        assertThat(article.getTagses()).containsOnly(articleTagBack);

        article.removeTags(articleTagBack);
        assertThat(article.getTagses()).doesNotContain(articleTagBack);

        article.tagses(new HashSet<>(Set.of(articleTagBack)));
        assertThat(article.getTagses()).containsOnly(articleTagBack);

        article.setTagses(new HashSet<>());
        assertThat(article.getTagses()).doesNotContain(articleTagBack);
    }
}
