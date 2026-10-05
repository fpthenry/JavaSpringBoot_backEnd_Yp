package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.GalleryTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
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
}
