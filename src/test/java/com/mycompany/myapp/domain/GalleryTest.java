package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.GalleryTestSamples.*;
import static com.mycompany.myapp.domain.ListingImageTestSamples.*;
import static com.mycompany.myapp.domain.ListingTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GalleryTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Gallery.class);
        Gallery gallery1 = getGallerySample1();
        Gallery gallery2 = new Gallery();
        assertThat(gallery1).isNotEqualTo(gallery2);

        gallery2.setId(gallery1.getId());
        assertThat(gallery1).isEqualTo(gallery2);

        gallery2 = getGallerySample2();
        assertThat(gallery1).isNotEqualTo(gallery2);
    }

    @Test
    void imagesTest() {
        Gallery gallery = getGalleryRandomSampleGenerator();
        ListingImage listingImageBack = getListingImageRandomSampleGenerator();

        gallery.addImages(listingImageBack);
        assertThat(gallery.getImageses()).containsOnly(listingImageBack);
        assertThat(listingImageBack.getGallery()).isEqualTo(gallery);

        gallery.removeImages(listingImageBack);
        assertThat(gallery.getImageses()).doesNotContain(listingImageBack);
        assertThat(listingImageBack.getGallery()).isNull();

        gallery.imageses(new HashSet<>(Set.of(listingImageBack)));
        assertThat(gallery.getImageses()).containsOnly(listingImageBack);
        assertThat(listingImageBack.getGallery()).isEqualTo(gallery);

        gallery.setImageses(new HashSet<>());
        assertThat(gallery.getImageses()).doesNotContain(listingImageBack);
        assertThat(listingImageBack.getGallery()).isNull();
    }

    @Test
    void listingTest() {
        Gallery gallery = getGalleryRandomSampleGenerator();
        Listing listingBack = getListingRandomSampleGenerator();

        gallery.setListing(listingBack);
        assertThat(gallery.getListing()).isEqualTo(listingBack);

        gallery.listing(null);
        assertThat(gallery.getListing()).isNull();
    }
}
