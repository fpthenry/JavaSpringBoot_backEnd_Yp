package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.ListingTestSamples.*;
import static com.mycompany.myapp.domain.LocationTestSamples.*;
import static com.mycompany.myapp.domain.LocationTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LocationTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Location.class);
        Location location1 = getLocationSample1();
        Location location2 = new Location();
        assertThat(location1).isNotEqualTo(location2);

        location2.setId(location1.getId());
        assertThat(location1).isEqualTo(location2);

        location2 = getLocationSample2();
        assertThat(location1).isNotEqualTo(location2);
    }

    @Test
    void parentTest() {
        Location location = getLocationRandomSampleGenerator();
        Location locationBack = getLocationRandomSampleGenerator();

        location.setParent(locationBack);
        assertThat(location.getParent()).isEqualTo(locationBack);

        location.parent(null);
        assertThat(location.getParent()).isNull();
    }

    @Test
    void listingTest() {
        Location location = getLocationRandomSampleGenerator();
        Listing listingBack = getListingRandomSampleGenerator();

        location.addListing(listingBack);
        assertThat(location.getListings()).containsOnly(listingBack);
        assertThat(listingBack.getLocations()).containsOnly(location);

        location.removeListing(listingBack);
        assertThat(location.getListings()).doesNotContain(listingBack);
        assertThat(listingBack.getLocations()).doesNotContain(location);

        location.listings(new HashSet<>(Set.of(listingBack)));
        assertThat(location.getListings()).containsOnly(listingBack);
        assertThat(listingBack.getLocations()).containsOnly(location);

        location.setListings(new HashSet<>());
        assertThat(location.getListings()).doesNotContain(listingBack);
        assertThat(listingBack.getLocations()).doesNotContain(location);
    }
}
