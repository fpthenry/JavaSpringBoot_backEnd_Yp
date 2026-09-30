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
        listingCriteria.title();
        listingCriteria.slug();
        listingCriteria.status();
        listingCriteria.email();
        listingCriteria.telephone();
        listingCriteria.mobile();
        listingCriteria.website();
        listingCriteria.fax();
        listingCriteria.taxCode();
        listingCriteria.nameAlias();
        listingCriteria.nameEn();
        listingCriteria.representative();
        listingCriteria.mainIndustry();
        listingCriteria.managedBy();
        listingCriteria.businessType();
        listingCriteria.statusYp();
        listingCriteria.foundedDate();
        listingCriteria.licenseModifiedDate();
        listingCriteria.latitude();
        listingCriteria.longitude();
        listingCriteria.apiId();
        listingCriteria.createdAt();
        listingCriteria.updatedAt();
        listingCriteria.imagesId();
        listingCriteria.galleriesId();
        listingCriteria.authorId();
        listingCriteria.categoriesId();
        listingCriteria.locationsId();
        listingCriteria.distinct();
    }

    private static Condition<ListingCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getTitle()) &&
                condition.apply(criteria.getSlug()) &&
                condition.apply(criteria.getStatus()) &&
                condition.apply(criteria.getEmail()) &&
                condition.apply(criteria.getTelephone()) &&
                condition.apply(criteria.getMobile()) &&
                condition.apply(criteria.getWebsite()) &&
                condition.apply(criteria.getFax()) &&
                condition.apply(criteria.getTaxCode()) &&
                condition.apply(criteria.getNameAlias()) &&
                condition.apply(criteria.getNameEn()) &&
                condition.apply(criteria.getRepresentative()) &&
                condition.apply(criteria.getMainIndustry()) &&
                condition.apply(criteria.getManagedBy()) &&
                condition.apply(criteria.getBusinessType()) &&
                condition.apply(criteria.getStatusYp()) &&
                condition.apply(criteria.getFoundedDate()) &&
                condition.apply(criteria.getLicenseModifiedDate()) &&
                condition.apply(criteria.getLatitude()) &&
                condition.apply(criteria.getLongitude()) &&
                condition.apply(criteria.getApiId()) &&
                condition.apply(criteria.getCreatedAt()) &&
                condition.apply(criteria.getUpdatedAt()) &&
                condition.apply(criteria.getImagesId()) &&
                condition.apply(criteria.getGalleriesId()) &&
                condition.apply(criteria.getAuthorId()) &&
                condition.apply(criteria.getCategoriesId()) &&
                condition.apply(criteria.getLocationsId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<ListingCriteria> copyFiltersAre(ListingCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getTitle(), copy.getTitle()) &&
                condition.apply(criteria.getSlug(), copy.getSlug()) &&
                condition.apply(criteria.getStatus(), copy.getStatus()) &&
                condition.apply(criteria.getEmail(), copy.getEmail()) &&
                condition.apply(criteria.getTelephone(), copy.getTelephone()) &&
                condition.apply(criteria.getMobile(), copy.getMobile()) &&
                condition.apply(criteria.getWebsite(), copy.getWebsite()) &&
                condition.apply(criteria.getFax(), copy.getFax()) &&
                condition.apply(criteria.getTaxCode(), copy.getTaxCode()) &&
                condition.apply(criteria.getNameAlias(), copy.getNameAlias()) &&
                condition.apply(criteria.getNameEn(), copy.getNameEn()) &&
                condition.apply(criteria.getRepresentative(), copy.getRepresentative()) &&
                condition.apply(criteria.getMainIndustry(), copy.getMainIndustry()) &&
                condition.apply(criteria.getManagedBy(), copy.getManagedBy()) &&
                condition.apply(criteria.getBusinessType(), copy.getBusinessType()) &&
                condition.apply(criteria.getStatusYp(), copy.getStatusYp()) &&
                condition.apply(criteria.getFoundedDate(), copy.getFoundedDate()) &&
                condition.apply(criteria.getLicenseModifiedDate(), copy.getLicenseModifiedDate()) &&
                condition.apply(criteria.getLatitude(), copy.getLatitude()) &&
                condition.apply(criteria.getLongitude(), copy.getLongitude()) &&
                condition.apply(criteria.getApiId(), copy.getApiId()) &&
                condition.apply(criteria.getCreatedAt(), copy.getCreatedAt()) &&
                condition.apply(criteria.getUpdatedAt(), copy.getUpdatedAt()) &&
                condition.apply(criteria.getImagesId(), copy.getImagesId()) &&
                condition.apply(criteria.getGalleriesId(), copy.getGalleriesId()) &&
                condition.apply(criteria.getAuthorId(), copy.getAuthorId()) &&
                condition.apply(criteria.getCategoriesId(), copy.getCategoriesId()) &&
                condition.apply(criteria.getLocationsId(), copy.getLocationsId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
