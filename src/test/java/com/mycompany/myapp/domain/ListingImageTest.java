package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.GalleryTestSamples.*;
import static com.mycompany.myapp.domain.ListingImageTestSamples.*;
import static com.mycompany.myapp.domain.ListingTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ListingImageTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ListingImage.class);
        ListingImage listingImage1 = getListingImageSample1();
        ListingImage listingImage2 = new ListingImage();
        assertThat(listingImage1).isNotEqualTo(listingImage2);

        listingImage2.setId(listingImage1.getId());
        assertThat(listingImage1).isEqualTo(listingImage2);

        listingImage2 = getListingImageSample2();
        assertThat(listingImage1).isNotEqualTo(listingImage2);
    }

    @Test
    void listingTest() {
        ListingImage listingImage = getListingImageRandomSampleGenerator();
        Listing listingBack = getListingRandomSampleGenerator();

        listingImage.setListing(listingBack);
        assertThat(listingImage.getListing()).isEqualTo(listingBack);

        listingImage.listing(null);
        assertThat(listingImage.getListing()).isNull();
    }

    @Test
    void galleryTest() {
        ListingImage listingImage = getListingImageRandomSampleGenerator();
        Gallery galleryBack = getGalleryRandomSampleGenerator();

        listingImage.setGallery(galleryBack);
        assertThat(listingImage.getGallery()).isEqualTo(galleryBack);

        listingImage.gallery(null);
        assertThat(listingImage.getGallery()).isNull();
    }
}
