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
    void childrenTest() {
        Location location = getLocationRandomSampleGenerator();
        Location locationBack = getLocationRandomSampleGenerator();

        location.addChildren(locationBack);
        assertThat(location.getChildrens()).containsOnly(locationBack);
        assertThat(locationBack.getParent()).isEqualTo(location);

        location.removeChildren(locationBack);
        assertThat(location.getChildrens()).doesNotContain(locationBack);
        assertThat(locationBack.getParent()).isNull();

        location.childrens(new HashSet<>(Set.of(locationBack)));
        assertThat(location.getChildrens()).containsOnly(locationBack);
        assertThat(locationBack.getParent()).isEqualTo(location);

        location.setChildrens(new HashSet<>());
        assertThat(location.getChildrens()).doesNotContain(locationBack);
        assertThat(locationBack.getParent()).isNull();
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
    void listingsTest() {
        Location location = getLocationRandomSampleGenerator();
        Listing listingBack = getListingRandomSampleGenerator();

        location.addListings(listingBack);
        assertThat(location.getListingses()).containsOnly(listingBack);
        assertThat(listingBack.getLocationses()).containsOnly(location);

        location.removeListings(listingBack);
        assertThat(location.getListingses()).doesNotContain(listingBack);
        assertThat(listingBack.getLocationses()).doesNotContain(location);

        location.listingses(new HashSet<>(Set.of(listingBack)));
        assertThat(location.getListingses()).containsOnly(listingBack);
        assertThat(listingBack.getLocationses()).containsOnly(location);

        location.setListingses(new HashSet<>());
        assertThat(location.getListingses()).doesNotContain(listingBack);
        assertThat(listingBack.getLocationses()).doesNotContain(location);
    }
}
