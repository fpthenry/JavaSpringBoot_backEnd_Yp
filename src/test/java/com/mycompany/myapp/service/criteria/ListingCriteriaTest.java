package com.mycompany.myapp.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class ListingCriteriaTest {

    @Test
    void newListingCriteriaHasAllFiltersNullTest() {
        var listingCriteria = new ListingCriteria();
        assertThat(listingCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void listingCriteriaFluentMethodsCreatesFiltersTest() {
        var listingCriteria = new ListingCriteria();

        setAllFilters(listingCriteria);

        assertThat(listingCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void listingCriteriaCopyCreatesNullFilterTest() {
        var listingCriteria = new ListingCriteria();
        var copy = listingCriteria.copy();

        assertThat(listingCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(listingCriteria)
        );
    }

    @Test
    void listingCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var listingCriteria = new ListingCriteria();
        setAllFilters(listingCriteria);

        var copy = listingCriteria.copy();

        assertThat(listingCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(listingCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var listingCriteria = new ListingCriteria();

        assertThat(listingCriteria).hasToString("ListingCriteria{}");
    }

    private static void setAllFilters(ListingCriteria listingCriteria) {
        listingCriteria.id();
        listingCriteria.wpId();
        listingCriteria.apiId();
        listingCriteria.name();
        listingCriteria.nameEn();
        listingCriteria.nameAlias();
        listingCriteria.slug();
        listingCriteria.phone();
        listingCriteria.mobile();
        listingCriteria.email();
        listingCriteria.website();
        listingCriteria.taxCode();
        listingCriteria.representative();
        listingCriteria.capital();
        listingCriteria.foundedYear();
        listingCriteria.businessType();
        listingCriteria.businessStatus();
        listingCriteria.industryCode();
        listingCriteria.managedBy();
        listingCriteria.thumbnail();
        listingCriteria.viewCount();
        listingCriteria.isFeatured();
        listingCriteria.status();
        listingCriteria.publishedAt();
        listingCriteria.modifiedAt();
        listingCriteria.createdAt();
        listingCriteria.updatedAt();
        listingCriteria.esIndexed();
        listingCriteria.categoryId();
        listingCriteria.locationId();
        listingCriteria.distinct();
    }

    private static Condition<ListingCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getWpId()) &&
                condition.apply(criteria.getApiId()) &&
                condition.apply(criteria.getName()) &&
                condition.apply(criteria.getNameEn()) &&
                condition.apply(criteria.getNameAlias()) &&
                condition.apply(criteria.getSlug()) &&
                condition.apply(criteria.getPhone()) &&
                condition.apply(criteria.getMobile()) &&
                condition.apply(criteria.getEmail()) &&
                condition.apply(criteria.getWebsite()) &&
                condition.apply(criteria.getTaxCode()) &&
                condition.apply(criteria.getRepresentative()) &&
                condition.apply(criteria.getCapital()) &&
                condition.apply(criteria.getFoundedYear()) &&
                condition.apply(criteria.getBusinessType()) &&
                condition.apply(criteria.getBusinessStatus()) &&
                condition.apply(criteria.getIndustryCode()) &&
                condition.apply(criteria.getManagedBy()) &&
                condition.apply(criteria.getThumbnail()) &&
                condition.apply(criteria.getViewCount()) &&
                condition.apply(criteria.getIsFeatured()) &&
                condition.apply(criteria.getStatus()) &&
                condition.apply(criteria.getPublishedAt()) &&
                condition.apply(criteria.getModifiedAt()) &&
                condition.apply(criteria.getCreatedAt()) &&
                condition.apply(criteria.getUpdatedAt()) &&
                condition.apply(criteria.getEsIndexed()) &&
                condition.apply(criteria.getCategoryId()) &&
                condition.apply(criteria.getLocationId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<ListingCriteria> copyFiltersAre(ListingCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getWpId(), copy.getWpId()) &&
                condition.apply(criteria.getApiId(), copy.getApiId()) &&
                condition.apply(criteria.getName(), copy.getName()) &&
                condition.apply(criteria.getNameEn(), copy.getNameEn()) &&
                condition.apply(criteria.getNameAlias(), copy.getNameAlias()) &&
                condition.apply(criteria.getSlug(), copy.getSlug()) &&
                condition.apply(criteria.getPhone(), copy.getPhone()) &&
                condition.apply(criteria.getMobile(), copy.getMobile()) &&
                condition.apply(criteria.getEmail(), copy.getEmail()) &&
                condition.apply(criteria.getWebsite(), copy.getWebsite()) &&
                condition.apply(criteria.getTaxCode(), copy.getTaxCode()) &&
                condition.apply(criteria.getRepresentative(), copy.getRepresentative()) &&
                condition.apply(criteria.getCapital(), copy.getCapital()) &&
                condition.apply(criteria.getFoundedYear(), copy.getFoundedYear()) &&
                condition.apply(criteria.getBusinessType(), copy.getBusinessType()) &&
                condition.apply(criteria.getBusinessStatus(), copy.getBusinessStatus()) &&
                condition.apply(criteria.getIndustryCode(), copy.getIndustryCode()) &&
                condition.apply(criteria.getManagedBy(), copy.getManagedBy()) &&
                condition.apply(criteria.getThumbnail(), copy.getThumbnail()) &&
                condition.apply(criteria.getViewCount(), copy.getViewCount()) &&
                condition.apply(criteria.getIsFeatured(), copy.getIsFeatured()) &&
                condition.apply(criteria.getStatus(), copy.getStatus()) &&
                condition.apply(criteria.getPublishedAt(), copy.getPublishedAt()) &&
                condition.apply(criteria.getModifiedAt(), copy.getModifiedAt()) &&
                condition.apply(criteria.getCreatedAt(), copy.getCreatedAt()) &&
                condition.apply(criteria.getUpdatedAt(), copy.getUpdatedAt()) &&
                condition.apply(criteria.getEsIndexed(), copy.getEsIndexed()) &&
                condition.apply(criteria.getCategoryId(), copy.getCategoryId()) &&
                condition.apply(criteria.getLocationId(), copy.getLocationId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
