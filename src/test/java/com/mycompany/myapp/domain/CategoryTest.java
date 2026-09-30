package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.CategoryTestSamples.*;
import static com.mycompany.myapp.domain.CategoryTestSamples.*;
import static com.mycompany.myapp.domain.ListingTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CategoryTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Category.class);
        Category category1 = getCategorySample1();
        Category category2 = new Category();
        assertThat(category1).isNotEqualTo(category2);

        category2.setId(category1.getId());
        assertThat(category1).isEqualTo(category2);

        category2 = getCategorySample2();
        assertThat(category1).isNotEqualTo(category2);
    }

    @Test
    void childrenTest() {
        Category category = getCategoryRandomSampleGenerator();
        Category categoryBack = getCategoryRandomSampleGenerator();

        category.addChildren(categoryBack);
        assertThat(category.getChildrens()).containsOnly(categoryBack);
        assertThat(categoryBack.getParent()).isEqualTo(category);

        category.removeChildren(categoryBack);
        assertThat(category.getChildrens()).doesNotContain(categoryBack);
        assertThat(categoryBack.getParent()).isNull();

        category.childrens(new HashSet<>(Set.of(categoryBack)));
        assertThat(category.getChildrens()).containsOnly(categoryBack);
        assertThat(categoryBack.getParent()).isEqualTo(category);

        category.setChildrens(new HashSet<>());
        assertThat(category.getChildrens()).doesNotContain(categoryBack);
        assertThat(categoryBack.getParent()).isNull();
    }

    @Test
    void parentTest() {
        Category category = getCategoryRandomSampleGenerator();
        Category categoryBack = getCategoryRandomSampleGenerator();

        category.setParent(categoryBack);
        assertThat(category.getParent()).isEqualTo(categoryBack);

        category.parent(null);
        assertThat(category.getParent()).isNull();
    }

    @Test
    void listingsTest() {
        Category category = getCategoryRandomSampleGenerator();
        Listing listingBack = getListingRandomSampleGenerator();

        category.addListings(listingBack);
        assertThat(category.getListingses()).containsOnly(listingBack);
        assertThat(listingBack.getCategorieses()).containsOnly(category);

        category.removeListings(listingBack);
        assertThat(category.getListingses()).doesNotContain(listingBack);
        assertThat(listingBack.getCategorieses()).doesNotContain(category);

        category.listingses(new HashSet<>(Set.of(listingBack)));
        assertThat(category.getListingses()).containsOnly(listingBack);
        assertThat(listingBack.getCategorieses()).containsOnly(category);

        category.setListingses(new HashSet<>());
        assertThat(category.getListingses()).doesNotContain(listingBack);
        assertThat(listingBack.getCategorieses()).doesNotContain(category);
    }
}
