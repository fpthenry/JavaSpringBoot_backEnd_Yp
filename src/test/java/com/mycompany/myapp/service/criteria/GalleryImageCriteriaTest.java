package com.mycompany.myapp.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class GalleryImageCriteriaTest {

    @Test
    void newGalleryImageCriteriaHasAllFiltersNullTest() {
        var galleryImageCriteria = new GalleryImageCriteria();
        assertThat(galleryImageCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void galleryImageCriteriaFluentMethodsCreatesFiltersTest() {
        var galleryImageCriteria = new GalleryImageCriteria();

        setAllFilters(galleryImageCriteria);

        assertThat(galleryImageCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void galleryImageCriteriaCopyCreatesNullFilterTest() {
        var galleryImageCriteria = new GalleryImageCriteria();
        var copy = galleryImageCriteria.copy();

        assertThat(galleryImageCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(galleryImageCriteria)
        );
    }

    @Test
    void galleryImageCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var galleryImageCriteria = new GalleryImageCriteria();
        setAllFilters(galleryImageCriteria);

        var copy = galleryImageCriteria.copy();

        assertThat(galleryImageCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(galleryImageCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var galleryImageCriteria = new GalleryImageCriteria();

        assertThat(galleryImageCriteria).hasToString("GalleryImageCriteria{}");
    }

    private static void setAllFilters(GalleryImageCriteria galleryImageCriteria) {
        galleryImageCriteria.id();
        galleryImageCriteria.title();
        galleryImageCriteria.imageUrl();
        galleryImageCriteria.linkUrl();
        galleryImageCriteria.altText();
        galleryImageCriteria.displayOrder();
        galleryImageCriteria.active();
        galleryImageCriteria.startAt();
        galleryImageCriteria.endAt();
        galleryImageCriteria.openInNewTab();
        galleryImageCriteria.galleryId();
        galleryImageCriteria.distinct();
    }

    private static Condition<GalleryImageCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getTitle()) &&
                condition.apply(criteria.getImageUrl()) &&
                condition.apply(criteria.getLinkUrl()) &&
                condition.apply(criteria.getAltText()) &&
                condition.apply(criteria.getDisplayOrder()) &&
                condition.apply(criteria.getActive()) &&
                condition.apply(criteria.getStartAt()) &&
                condition.apply(criteria.getEndAt()) &&
                condition.apply(criteria.getOpenInNewTab()) &&
                condition.apply(criteria.getGalleryId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<GalleryImageCriteria> copyFiltersAre(
        GalleryImageCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getTitle(), copy.getTitle()) &&
                condition.apply(criteria.getImageUrl(), copy.getImageUrl()) &&
                condition.apply(criteria.getLinkUrl(), copy.getLinkUrl()) &&
                condition.apply(criteria.getAltText(), copy.getAltText()) &&
                condition.apply(criteria.getDisplayOrder(), copy.getDisplayOrder()) &&
                condition.apply(criteria.getActive(), copy.getActive()) &&
                condition.apply(criteria.getStartAt(), copy.getStartAt()) &&
                condition.apply(criteria.getEndAt(), copy.getEndAt()) &&
                condition.apply(criteria.getOpenInNewTab(), copy.getOpenInNewTab()) &&
                condition.apply(criteria.getGalleryId(), copy.getGalleryId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
