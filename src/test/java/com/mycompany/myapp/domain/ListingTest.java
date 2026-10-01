package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.CategoryTestSamples.*;
import static com.mycompany.myapp.domain.ListingTestSamples.*;
import static com.mycompany.myapp.domain.LocationTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ListingTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Listing.class);
        Listing listing1 = getListingSample1();
        Listing listing2 = new Listing();
        assertThat(listing1).isNotEqualTo(listing2);

        listing2.setId(listing1.getId());
        assertThat(listing1).isEqualTo(listing2);

        listing2 = getListingSample2();
        assertThat(listing1).isNotEqualTo(listing2);
    }

    @Test
    void categoryTest() {
        Listing listing = getListingRandomSampleGenerator();
        Category categoryBack = getCategoryRandomSampleGenerator();

        listing.addCategory(categoryBack);
        assertThat(listing.getCategories()).containsOnly(categoryBack);

        listing.removeCategory(categoryBack);
        assertThat(listing.getCategories()).doesNotContain(categoryBack);

        listing.categories(new HashSet<>(Set.of(categoryBack)));
        assertThat(listing.getCategories()).containsOnly(categoryBack);

        listing.setCategories(new HashSet<>());
        assertThat(listing.getCategories()).doesNotContain(categoryBack);
    }

    @Test
    void locationTest() {
        Listing listing = getListingRandomSampleGenerator();
        Location locationBack = getLocationRandomSampleGenerator();

        listing.addLocation(locationBack);
        assertThat(listing.getLocations()).containsOnly(locationBack);

        listing.removeLocation(locationBack);
        assertThat(listing.getLocations()).doesNotContain(locationBack);

        listing.locations(new HashSet<>(Set.of(locationBack)));
        assertThat(listing.getLocations()).containsOnly(locationBack);

        listing.setLocations(new HashSet<>());
        assertThat(listing.getLocations()).doesNotContain(locationBack);
    }
}
