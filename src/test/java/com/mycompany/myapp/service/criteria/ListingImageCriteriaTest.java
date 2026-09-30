package com.mycompany.myapp.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class ListingImageCriteriaTest {

    @Test
    void newListingImageCriteriaHasAllFiltersNullTest() {
        var listingImageCriteria = new ListingImageCriteria();
        assertThat(listingImageCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void listingImageCriteriaFluentMethodsCreatesFiltersTest() {
        var listingImageCriteria = new ListingImageCriteria();

        setAllFilters(listingImageCriteria);

        assertThat(listingImageCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void listingImageCriteriaCopyCreatesNullFilterTest() {
        var listingImageCriteria = new ListingImageCriteria();
        var copy = listingImageCriteria.copy();

        assertThat(listingImageCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(listingImageCriteria)
        );
    }

    @Test
    void listingImageCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var listingImageCriteria = new ListingImageCriteria();
        setAllFilters(listingImageCriteria);

        var copy = listingImageCriteria.copy();

        assertThat(listingImageCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(listingImageCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var listingImageCriteria = new ListingImageCriteria();

        assertThat(listingImageCriteria).hasToString("ListingImageCriteria{}");
    }

    private static void setAllFilters(ListingImageCriteria listingImageCriteria) {
        listingImageCriteria.id();
        listingImageCriteria.altText();
        listingImageCriteria.displayOrder();
        listingImageCriteria.isFeatured();
        listingImageCriteria.createdAt();
        listingImageCriteria.updatedAt();
        listingImageCriteria.listingId();
        listingImageCriteria.galleryId();
        listingImageCriteria.distinct();
    }

    private static Condition<ListingImageCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getAltText()) &&
                condition.apply(criteria.getDisplayOrder()) &&
                condition.apply(criteria.getIsFeatured()) &&
                condition.apply(criteria.getCreatedAt()) &&
                condition.apply(criteria.getUpdatedAt()) &&
                condition.apply(criteria.getListingId()) &&
                condition.apply(criteria.getGalleryId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<ListingImageCriteria> copyFiltersAre(
        ListingImageCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getAltText(), copy.getAltText()) &&
                condition.apply(criteria.getDisplayOrder(), copy.getDisplayOrder()) &&
                condition.apply(criteria.getIsFeatured(), copy.getIsFeatured()) &&
                condition.apply(criteria.getCreatedAt(), copy.getCreatedAt()) &&
                condition.apply(criteria.getUpdatedAt(), copy.getUpdatedAt()) &&
                condition.apply(criteria.getListingId(), copy.getListingId()) &&
                condition.apply(criteria.getGalleryId(), copy.getGalleryId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
