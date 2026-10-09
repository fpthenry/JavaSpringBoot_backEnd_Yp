package com.mycompany.myapp.repository.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class ListingSearchTest {

    @Test
    void idKemMoiCapChaVaKhongTreoKhiCayCoVong() {
        // Hà Nội mới 11960 > Vĩnh Tuy 12000; Hà Nội cũ 2 > Hai Bà Trưng 30 > Thanh Lương 800; 8 <-> 9 vòng lặp
        Map<Long, Long> parents = new HashMap<>();
        parents.put(11960L, null);
        parents.put(12000L, 11960L);
        parents.put(2L, null);
        parents.put(30L, 2L);
        parents.put(800L, 30L);
        parents.put(8L, 9L);
        parents.put(9L, 8L);
        assertThat(ListingSearchFields.withAncestors(List.of(12000L, 800L), parents)).containsExactlyInAnyOrder(
            12000L,
            11960L,
            800L,
            30L,
            2L
        );
        assertThat(ListingSearchFields.withAncestors(List.of(8L), parents)).containsExactlyInAnyOrder(8L, 9L);
        assertThat(ListingSearchFields.withAncestors(List.of(), parents)).isEmpty();
    }

    @Test
    void sapXepIdTheoSoFieldChuTheoKeyword() {
        Pageable mapped = ListingSearchRepositoryInternalImpl.toEsPageable(
            PageRequest.of(2, 20, Sort.by(Sort.Order.asc("id"), Sort.Order.desc("createdAt"), Sort.Order.asc("name")))
        );
        assertThat(mapped.getPageNumber()).isEqualTo(2);
        assertThat(mapped.getSort()).containsExactly(
            Sort.Order.asc("sortId"),
            Sort.Order.desc("createdAt"),
            Sort.Order.asc("name.keyword")
        );
        assertThat(ListingSearchRepositoryInternalImpl.toEsPageable(PageRequest.of(0, 20)).getSort().isUnsorted()).isTrue();
    }

    @Test
    void truyVanTuKhoaVaLocTheoCay() {
        String json = ListingSearchRepositoryInternalImpl.buildQuery(new ListingSearchFilter(" lãng yên ", 12000L, 4L)).toString();
        assertThat(json)
            .contains("\"query\":\"lãng yên\"")
            .contains("\"default_operator\":\"and\"")
            .contains("{\"term\":{\"locationIds\":{\"value\":12000}}}")
            .contains("{\"term\":{\"categoryIds\":{\"value\":4}}}");
        String onlyFilter = ListingSearchRepositoryInternalImpl.buildQuery(new ListingSearchFilter(null, 11960L, null)).toString();
        assertThat(onlyFilter).contains("match_all").contains("locationIds").doesNotContain("query_string").doesNotContain("categoryIds");
    }
}
