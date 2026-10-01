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
    void parentTest() {
        Category category = getCategoryRandomSampleGenerator();
        Category categoryBack = getCategoryRandomSampleGenerator();

        category.setParent(categoryBack);
        assertThat(category.getParent()).isEqualTo(categoryBack);

        category.parent(null);
        assertThat(category.getParent()).isNull();
    }

    @Test
    void listingTest() {
        Category category = getCategoryRandomSampleGenerator();
        Listing listingBack = getListingRandomSampleGenerator();

        category.addListing(listingBack);
        assertThat(category.getListings()).containsOnly(listingBack);
        assertThat(listingBack.getCategories()).containsOnly(category);

        category.removeListing(listingBack);
        assertThat(category.getListings()).doesNotContain(listingBack);
        assertThat(listingBack.getCategories()).doesNotContain(category);

        category.listings(new HashSet<>(Set.of(listingBack)));
        assertThat(category.getListings()).containsOnly(listingBack);
        assertThat(listingBack.getCategories()).containsOnly(category);

        category.setListings(new HashSet<>());
        assertThat(category.getListings()).doesNotContain(listingBack);
        assertThat(listingBack.getCategories()).doesNotContain(category);
    }
}
