package com.mycompany.myapp.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class RedirectRuleCriteriaTest {

    @Test
    void newRedirectRuleCriteriaHasAllFiltersNullTest() {
        var redirectRuleCriteria = new RedirectRuleCriteria();
        assertThat(redirectRuleCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void redirectRuleCriteriaFluentMethodsCreatesFiltersTest() {
        var redirectRuleCriteria = new RedirectRuleCriteria();

        setAllFilters(redirectRuleCriteria);

        assertThat(redirectRuleCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void redirectRuleCriteriaCopyCreatesNullFilterTest() {
        var redirectRuleCriteria = new RedirectRuleCriteria();
        var copy = redirectRuleCriteria.copy();

        assertThat(redirectRuleCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(redirectRuleCriteria)
        );
    }

    @Test
    void redirectRuleCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var redirectRuleCriteria = new RedirectRuleCriteria();
        setAllFilters(redirectRuleCriteria);

        var copy = redirectRuleCriteria.copy();

        assertThat(redirectRuleCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(redirectRuleCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var redirectRuleCriteria = new RedirectRuleCriteria();

        assertThat(redirectRuleCriteria).hasToString("RedirectRuleCriteria{}");
    }

    private static void setAllFilters(RedirectRuleCriteria redirectRuleCriteria) {
        redirectRuleCriteria.id();
        redirectRuleCriteria.sourceId();
        redirectRuleCriteria.sourceSlug();
        redirectRuleCriteria.destinationId();
        redirectRuleCriteria.destinationSlug();
        redirectRuleCriteria.objectType();
        redirectRuleCriteria.distinct();
    }

    private static Condition<RedirectRuleCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getSourceId()) &&
                condition.apply(criteria.getSourceSlug()) &&
                condition.apply(criteria.getDestinationId()) &&
                condition.apply(criteria.getDestinationSlug()) &&
                condition.apply(criteria.getObjectType()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<RedirectRuleCriteria> copyFiltersAre(
        RedirectRuleCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getSourceId(), copy.getSourceId()) &&
                condition.apply(criteria.getSourceSlug(), copy.getSourceSlug()) &&
                condition.apply(criteria.getDestinationId(), copy.getDestinationId()) &&
                condition.apply(criteria.getDestinationSlug(), copy.getDestinationSlug()) &&
                condition.apply(criteria.getObjectType(), copy.getObjectType()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
