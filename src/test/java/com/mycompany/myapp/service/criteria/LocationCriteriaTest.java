package com.mycompany.myapp.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class LocationCriteriaTest {

    @Test
    void newLocationCriteriaHasAllFiltersNullTest() {
        var locationCriteria = new LocationCriteria();
        assertThat(locationCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void locationCriteriaFluentMethodsCreatesFiltersTest() {
        var locationCriteria = new LocationCriteria();

        setAllFilters(locationCriteria);

        assertThat(locationCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void locationCriteriaCopyCreatesNullFilterTest() {
        var locationCriteria = new LocationCriteria();
        var copy = locationCriteria.copy();

        assertThat(locationCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(locationCriteria)
        );
    }

    @Test
    void locationCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var locationCriteria = new LocationCriteria();
        setAllFilters(locationCriteria);

        var copy = locationCriteria.copy();

        assertThat(locationCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(locationCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var locationCriteria = new LocationCriteria();

        assertThat(locationCriteria).hasToString("LocationCriteria{}");
    }

    private static void setAllFilters(LocationCriteria locationCriteria) {
        locationCriteria.id();
        locationCriteria.name();
        locationCriteria.slug();
        locationCriteria.type();
        locationCriteria.provinceCode();
        locationCriteria.districtCode();
        locationCriteria.wardCode();
        locationCriteria.count();
        locationCriteria.displayOrder();
        locationCriteria.createdAt();
        locationCriteria.updatedAt();
        locationCriteria.childrenId();
        locationCriteria.parentId();
        locationCriteria.listingsId();
        locationCriteria.distinct();
    }

    private static Condition<LocationCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getName()) &&
                condition.apply(criteria.getSlug()) &&
                condition.apply(criteria.getType()) &&
                condition.apply(criteria.getProvinceCode()) &&
                condition.apply(criteria.getDistrictCode()) &&
                condition.apply(criteria.getWardCode()) &&
                condition.apply(criteria.getCount()) &&
                condition.apply(criteria.getDisplayOrder()) &&
                condition.apply(criteria.getCreatedAt()) &&
                condition.apply(criteria.getUpdatedAt()) &&
                condition.apply(criteria.getChildrenId()) &&
                condition.apply(criteria.getParentId()) &&
                condition.apply(criteria.getListingsId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<LocationCriteria> copyFiltersAre(LocationCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getName(), copy.getName()) &&
                condition.apply(criteria.getSlug(), copy.getSlug()) &&
                condition.apply(criteria.getType(), copy.getType()) &&
                condition.apply(criteria.getProvinceCode(), copy.getProvinceCode()) &&
                condition.apply(criteria.getDistrictCode(), copy.getDistrictCode()) &&
                condition.apply(criteria.getWardCode(), copy.getWardCode()) &&
                condition.apply(criteria.getCount(), copy.getCount()) &&
                condition.apply(criteria.getDisplayOrder(), copy.getDisplayOrder()) &&
                condition.apply(criteria.getCreatedAt(), copy.getCreatedAt()) &&
                condition.apply(criteria.getUpdatedAt(), copy.getUpdatedAt()) &&
                condition.apply(criteria.getChildrenId(), copy.getChildrenId()) &&
                condition.apply(criteria.getParentId(), copy.getParentId()) &&
                condition.apply(criteria.getListingsId(), copy.getListingsId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
