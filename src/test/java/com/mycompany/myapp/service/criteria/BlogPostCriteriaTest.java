package com.mycompany.myapp.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class BlogPostCriteriaTest {

    @Test
    void newBlogPostCriteriaHasAllFiltersNullTest() {
        var blogPostCriteria = new BlogPostCriteria();
        assertThat(blogPostCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void blogPostCriteriaFluentMethodsCreatesFiltersTest() {
        var blogPostCriteria = new BlogPostCriteria();

        setAllFilters(blogPostCriteria);

        assertThat(blogPostCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void blogPostCriteriaCopyCreatesNullFilterTest() {
        var blogPostCriteria = new BlogPostCriteria();
        var copy = blogPostCriteria.copy();

        assertThat(blogPostCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(blogPostCriteria)
        );
    }

    @Test
    void blogPostCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var blogPostCriteria = new BlogPostCriteria();
        setAllFilters(blogPostCriteria);

        var copy = blogPostCriteria.copy();

        assertThat(blogPostCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(blogPostCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var blogPostCriteria = new BlogPostCriteria();

        assertThat(blogPostCriteria).hasToString("BlogPostCriteria{}");
    }

    private static void setAllFilters(BlogPostCriteria blogPostCriteria) {
        blogPostCriteria.id();
        blogPostCriteria.wpId();
        blogPostCriteria.title();
        blogPostCriteria.slug();
        blogPostCriteria.thumbnail();
        blogPostCriteria.status();
        blogPostCriteria.viewCount();
        blogPostCriteria.publishedAt();
        blogPostCriteria.createdAt();
        blogPostCriteria.updatedAt();
        blogPostCriteria.authorName();
        blogPostCriteria.categoryId();
        blogPostCriteria.tagId();
        blogPostCriteria.distinct();
    }

    private static Condition<BlogPostCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getWpId()) &&
                condition.apply(criteria.getTitle()) &&
                condition.apply(criteria.getSlug()) &&
                condition.apply(criteria.getThumbnail()) &&
                condition.apply(criteria.getStatus()) &&
                condition.apply(criteria.getViewCount()) &&
                condition.apply(criteria.getPublishedAt()) &&
                condition.apply(criteria.getCreatedAt()) &&
                condition.apply(criteria.getUpdatedAt()) &&
                condition.apply(criteria.getAuthorName()) &&
                condition.apply(criteria.getCategoryId()) &&
                condition.apply(criteria.getTagId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<BlogPostCriteria> copyFiltersAre(BlogPostCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getWpId(), copy.getWpId()) &&
                condition.apply(criteria.getTitle(), copy.getTitle()) &&
                condition.apply(criteria.getSlug(), copy.getSlug()) &&
                condition.apply(criteria.getThumbnail(), copy.getThumbnail()) &&
                condition.apply(criteria.getStatus(), copy.getStatus()) &&
                condition.apply(criteria.getViewCount(), copy.getViewCount()) &&
                condition.apply(criteria.getPublishedAt(), copy.getPublishedAt()) &&
                condition.apply(criteria.getCreatedAt(), copy.getCreatedAt()) &&
                condition.apply(criteria.getUpdatedAt(), copy.getUpdatedAt()) &&
                condition.apply(criteria.getAuthorName(), copy.getAuthorName()) &&
                condition.apply(criteria.getCategoryId(), copy.getCategoryId()) &&
                condition.apply(criteria.getTagId(), copy.getTagId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
