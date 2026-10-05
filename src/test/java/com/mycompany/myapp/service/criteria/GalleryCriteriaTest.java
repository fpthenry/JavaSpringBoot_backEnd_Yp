package com.mycompany.myapp.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class GalleryCriteriaTest {

    @Test
    void newGalleryCriteriaHasAllFiltersNullTest() {
        var galleryCriteria = new GalleryCriteria();
        assertThat(galleryCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void galleryCriteriaFluentMethodsCreatesFiltersTest() {
        var galleryCriteria = new GalleryCriteria();

        setAllFilters(galleryCriteria);

        assertThat(galleryCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void galleryCriteriaCopyCreatesNullFilterTest() {
        var galleryCriteria = new GalleryCriteria();
        var copy = galleryCriteria.copy();

        assertThat(galleryCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(galleryCriteria)
        );
    }

    @Test
    void galleryCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var galleryCriteria = new GalleryCriteria();
        setAllFilters(galleryCriteria);

        var copy = galleryCriteria.copy();

        assertThat(galleryCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(galleryCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var galleryCriteria = new GalleryCriteria();

        assertThat(galleryCriteria).hasToString("GalleryCriteria{}");
    }

    private static void setAllFilters(GalleryCriteria galleryCriteria) {
        galleryCriteria.id();
        galleryCriteria.name();
        galleryCriteria.code();
        galleryCriteria.active();
        galleryCriteria.wpId();
        galleryCriteria.distinct();
    }

    private static Condition<GalleryCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getName()) &&
                condition.apply(criteria.getCode()) &&
                condition.apply(criteria.getActive()) &&
                condition.apply(criteria.getWpId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<GalleryCriteria> copyFiltersAre(GalleryCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getName(), copy.getName()) &&
                condition.apply(criteria.getCode(), copy.getCode()) &&
                condition.apply(criteria.getActive(), copy.getActive()) &&
                condition.apply(criteria.getWpId(), copy.getWpId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
