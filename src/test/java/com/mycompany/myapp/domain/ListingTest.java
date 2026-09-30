package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.CategoryTestSamples.*;
import static com.mycompany.myapp.domain.GalleryTestSamples.*;
import static com.mycompany.myapp.domain.ListingImageTestSamples.*;
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
    void imagesTest() {
        Listing listing = getListingRandomSampleGenerator();
        ListingImage listingImageBack = getListingImageRandomSampleGenerator();

        listing.addImages(listingImageBack);
        assertThat(listing.getImageses()).containsOnly(listingImageBack);
        assertThat(listingImageBack.getListing()).isEqualTo(listing);

        listing.removeImages(listingImageBack);
        assertThat(listing.getImageses()).doesNotContain(listingImageBack);
        assertThat(listingImageBack.getListing()).isNull();

        listing.imageses(new HashSet<>(Set.of(listingImageBack)));
        assertThat(listing.getImageses()).containsOnly(listingImageBack);
        assertThat(listingImageBack.getListing()).isEqualTo(listing);

        listing.setImageses(new HashSet<>());
        assertThat(listing.getImageses()).doesNotContain(listingImageBack);
        assertThat(listingImageBack.getListing()).isNull();
    }

    @Test
    void galleriesTest() {
        Listing listing = getListingRandomSampleGenerator();
        Gallery galleryBack = getGalleryRandomSampleGenerator();

        listing.addGalleries(galleryBack);
        assertThat(listing.getGallerieses()).containsOnly(galleryBack);
        assertThat(galleryBack.getListing()).isEqualTo(listing);

        listing.removeGalleries(galleryBack);
        assertThat(listing.getGallerieses()).doesNotContain(galleryBack);
        assertThat(galleryBack.getListing()).isNull();

        listing.gallerieses(new HashSet<>(Set.of(galleryBack)));
        assertThat(listing.getGallerieses()).containsOnly(galleryBack);
        assertThat(galleryBack.getListing()).isEqualTo(listing);

        listing.setGallerieses(new HashSet<>());
        assertThat(listing.getGallerieses()).doesNotContain(galleryBack);
        assertThat(galleryBack.getListing()).isNull();
    }

    @Test
    void categoriesTest() {
        Listing listing = getListingRandomSampleGenerator();
        Category categoryBack = getCategoryRandomSampleGenerator();

        listing.addCategories(categoryBack);
        assertThat(listing.getCategorieses()).containsOnly(categoryBack);

        listing.removeCategories(categoryBack);
        assertThat(listing.getCategorieses()).doesNotContain(categoryBack);

        listing.categorieses(new HashSet<>(Set.of(categoryBack)));
        assertThat(listing.getCategorieses()).containsOnly(categoryBack);

        listing.setCategorieses(new HashSet<>());
        assertThat(listing.getCategorieses()).doesNotContain(categoryBack);
    }

    @Test
    void locationsTest() {
        Listing listing = getListingRandomSampleGenerator();
        Location locationBack = getLocationRandomSampleGenerator();

        listing.addLocations(locationBack);
        assertThat(listing.getLocationses()).containsOnly(locationBack);

        listing.removeLocations(locationBack);
        assertThat(listing.getLocationses()).doesNotContain(locationBack);

        listing.locationses(new HashSet<>(Set.of(locationBack)));
        assertThat(listing.getLocationses()).containsOnly(locationBack);

        listing.setLocationses(new HashSet<>());
        assertThat(listing.getLocationses()).doesNotContain(locationBack);
    }
}
