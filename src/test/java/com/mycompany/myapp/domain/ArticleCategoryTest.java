package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.ArticleCategoryTestSamples.*;
import static com.mycompany.myapp.domain.ArticleCategoryTestSamples.*;
import static com.mycompany.myapp.domain.ArticleTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ArticleCategoryTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ArticleCategory.class);
        ArticleCategory articleCategory1 = getArticleCategorySample1();
        ArticleCategory articleCategory2 = new ArticleCategory();
        assertThat(articleCategory1).isNotEqualTo(articleCategory2);

        articleCategory2.setId(articleCategory1.getId());
        assertThat(articleCategory1).isEqualTo(articleCategory2);

        articleCategory2 = getArticleCategorySample2();
        assertThat(articleCategory1).isNotEqualTo(articleCategory2);
    }

    @Test
    void childrenTest() {
        ArticleCategory articleCategory = getArticleCategoryRandomSampleGenerator();
        ArticleCategory articleCategoryBack = getArticleCategoryRandomSampleGenerator();

        articleCategory.addChildren(articleCategoryBack);
        assertThat(articleCategory.getChildrens()).containsOnly(articleCategoryBack);
        assertThat(articleCategoryBack.getParent()).isEqualTo(articleCategory);

        articleCategory.removeChildren(articleCategoryBack);
        assertThat(articleCategory.getChildrens()).doesNotContain(articleCategoryBack);
        assertThat(articleCategoryBack.getParent()).isNull();

        articleCategory.childrens(new HashSet<>(Set.of(articleCategoryBack)));
        assertThat(articleCategory.getChildrens()).containsOnly(articleCategoryBack);
        assertThat(articleCategoryBack.getParent()).isEqualTo(articleCategory);

        articleCategory.setChildrens(new HashSet<>());
        assertThat(articleCategory.getChildrens()).doesNotContain(articleCategoryBack);
        assertThat(articleCategoryBack.getParent()).isNull();
    }

    @Test
    void parentTest() {
        ArticleCategory articleCategory = getArticleCategoryRandomSampleGenerator();
        ArticleCategory articleCategoryBack = getArticleCategoryRandomSampleGenerator();

        articleCategory.setParent(articleCategoryBack);
        assertThat(articleCategory.getParent()).isEqualTo(articleCategoryBack);

        articleCategory.parent(null);
        assertThat(articleCategory.getParent()).isNull();
    }

    @Test
    void articlesTest() {
        ArticleCategory articleCategory = getArticleCategoryRandomSampleGenerator();
        Article articleBack = getArticleRandomSampleGenerator();

        articleCategory.addArticles(articleBack);
        assertThat(articleCategory.getArticleses()).containsOnly(articleBack);
        assertThat(articleBack.getCategorieses()).containsOnly(articleCategory);

        articleCategory.removeArticles(articleBack);
        assertThat(articleCategory.getArticleses()).doesNotContain(articleBack);
        assertThat(articleBack.getCategorieses()).doesNotContain(articleCategory);

        articleCategory.articleses(new HashSet<>(Set.of(articleBack)));
        assertThat(articleCategory.getArticleses()).containsOnly(articleBack);
        assertThat(articleBack.getCategorieses()).containsOnly(articleCategory);

        articleCategory.setArticleses(new HashSet<>());
        assertThat(articleCategory.getArticleses()).doesNotContain(articleBack);
        assertThat(articleBack.getCategorieses()).doesNotContain(articleCategory);
    }
}
