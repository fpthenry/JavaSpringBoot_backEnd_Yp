package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.GalleryImageTestSamples.*;
import static com.mycompany.myapp.domain.GalleryTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class GalleryImageTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(GalleryImage.class);
        GalleryImage galleryImage1 = getGalleryImageSample1();
        GalleryImage galleryImage2 = new GalleryImage();
        assertThat(galleryImage1).isNotEqualTo(galleryImage2);

        galleryImage2.setId(galleryImage1.getId());
        assertThat(galleryImage1).isEqualTo(galleryImage2);

        galleryImage2 = getGalleryImageSample2();
        assertThat(galleryImage1).isNotEqualTo(galleryImage2);
    }

    @Test
    void galleryTest() {
        GalleryImage galleryImage = getGalleryImageRandomSampleGenerator();
        Gallery galleryBack = getGalleryRandomSampleGenerator();

        galleryImage.setGallery(galleryBack);
        assertThat(galleryImage.getGallery()).isEqualTo(galleryBack);

        galleryImage.gallery(null);
        assertThat(galleryImage.getGallery()).isNull();
    }
}
