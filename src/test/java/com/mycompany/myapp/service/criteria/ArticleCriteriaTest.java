package com.mycompany.myapp.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class ArticleCriteriaTest {

    @Test
    void newArticleCriteriaHasAllFiltersNullTest() {
        var articleCriteria = new ArticleCriteria();
        assertThat(articleCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void articleCriteriaFluentMethodsCreatesFiltersTest() {
        var articleCriteria = new ArticleCriteria();

        setAllFilters(articleCriteria);

        assertThat(articleCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void articleCriteriaCopyCreatesNullFilterTest() {
        var articleCriteria = new ArticleCriteria();
        var copy = articleCriteria.copy();

        assertThat(articleCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(articleCriteria)
        );
    }

    @Test
    void articleCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var articleCriteria = new ArticleCriteria();
        setAllFilters(articleCriteria);

        var copy = articleCriteria.copy();

        assertThat(articleCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(articleCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var articleCriteria = new ArticleCriteria();

        assertThat(articleCriteria).hasToString("ArticleCriteria{}");
    }

    private static void setAllFilters(ArticleCriteria articleCriteria) {
        articleCriteria.id();
        articleCriteria.title();
        articleCriteria.slug();
        articleCriteria.status();
        articleCriteria.createdAt();
        articleCriteria.updatedAt();
        articleCriteria.authorId();
        articleCriteria.categoriesId();
        articleCriteria.tagsId();
        articleCriteria.distinct();
    }

    private static Condition<ArticleCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getTitle()) &&
                condition.apply(criteria.getSlug()) &&
                condition.apply(criteria.getStatus()) &&
                condition.apply(criteria.getCreatedAt()) &&
                condition.apply(criteria.getUpdatedAt()) &&
                condition.apply(criteria.getAuthorId()) &&
                condition.apply(criteria.getCategoriesId()) &&
                condition.apply(criteria.getTagsId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<ArticleCriteria> copyFiltersAre(ArticleCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getTitle(), copy.getTitle()) &&
                condition.apply(criteria.getSlug(), copy.getSlug()) &&
                condition.apply(criteria.getStatus(), copy.getStatus()) &&
                condition.apply(criteria.getCreatedAt(), copy.getCreatedAt()) &&
                condition.apply(criteria.getUpdatedAt(), copy.getUpdatedAt()) &&
                condition.apply(criteria.getAuthorId(), copy.getAuthorId()) &&
                condition.apply(criteria.getCategoriesId(), copy.getCategoriesId()) &&
                condition.apply(criteria.getTagsId(), copy.getTagsId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
