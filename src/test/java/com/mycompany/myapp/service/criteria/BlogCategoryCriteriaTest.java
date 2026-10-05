package com.mycompany.myapp.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class BlogCategoryCriteriaTest {

    @Test
    void newBlogCategoryCriteriaHasAllFiltersNullTest() {
        var blogCategoryCriteria = new BlogCategoryCriteria();
        assertThat(blogCategoryCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void blogCategoryCriteriaFluentMethodsCreatesFiltersTest() {
        var blogCategoryCriteria = new BlogCategoryCriteria();

        setAllFilters(blogCategoryCriteria);

        assertThat(blogCategoryCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void blogCategoryCriteriaCopyCreatesNullFilterTest() {
        var blogCategoryCriteria = new BlogCategoryCriteria();
        var copy = blogCategoryCriteria.copy();

        assertThat(blogCategoryCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(blogCategoryCriteria)
        );
    }

    @Test
    void blogCategoryCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var blogCategoryCriteria = new BlogCategoryCriteria();
        setAllFilters(blogCategoryCriteria);

        var copy = blogCategoryCriteria.copy();

        assertThat(blogCategoryCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(blogCategoryCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var blogCategoryCriteria = new BlogCategoryCriteria();

        assertThat(blogCategoryCriteria).hasToString("BlogCategoryCriteria{}");
    }

    private static void setAllFilters(BlogCategoryCriteria blogCategoryCriteria) {
        blogCategoryCriteria.id();
        blogCategoryCriteria.wpTermId();
        blogCategoryCriteria.name();
        blogCategoryCriteria.slug();
        blogCategoryCriteria.postCount();
        blogCategoryCriteria.parentId();
        blogCategoryCriteria.blogPostId();
        blogCategoryCriteria.distinct();
    }

    private static Condition<BlogCategoryCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getWpTermId()) &&
                condition.apply(criteria.getName()) &&
                condition.apply(criteria.getSlug()) &&
                condition.apply(criteria.getPostCount()) &&
                condition.apply(criteria.getParentId()) &&
                condition.apply(criteria.getBlogPostId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<BlogCategoryCriteria> copyFiltersAre(
        BlogCategoryCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getWpTermId(), copy.getWpTermId()) &&
                condition.apply(criteria.getName(), copy.getName()) &&
                condition.apply(criteria.getSlug(), copy.getSlug()) &&
                condition.apply(criteria.getPostCount(), copy.getPostCount()) &&
                condition.apply(criteria.getParentId(), copy.getParentId()) &&
                condition.apply(criteria.getBlogPostId(), copy.getBlogPostId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
