package com.mycompany.myapp.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.service.criteria.ListingCriteria;
import org.junit.jupiter.api.Test;
import tech.jhipster.service.filter.LongFilter;
import tech.jhipster.service.filter.StringFilter;

class ListingQueryServiceTreeFilterTest {

    private static LongFilter equalsTo(long id) {
        LongFilter filter = new LongFilter();
        filter.setEquals(id);
        return filter;
    }

    @Test
    void chiCoBoLocCayThiDungElasticsearch() {
        ListingCriteria criteria = new ListingCriteria();
        criteria.setLocationTreeId(equalsTo(12000));
        criteria.setCategoryTreeId(equalsTo(4));
        assertThat(ListingQueryService.onlyTreeFilters(criteria)).isTrue();
    }

    @Test
    void coBoLocKhacThiVanDungMySql() {
        ListingCriteria criteria = new ListingCriteria();
        criteria.setLocationTreeId(equalsTo(12000));
        StringFilter status = new StringFilter();
        status.setEquals("publish");
        criteria.setStatus(status);
        assertThat(ListingQueryService.onlyTreeFilters(criteria)).isFalse();
    }
}
