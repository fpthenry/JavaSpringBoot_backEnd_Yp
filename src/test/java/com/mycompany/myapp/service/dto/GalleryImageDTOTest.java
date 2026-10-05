package com.mycompany.myapp.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class GalleryImageDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(GalleryImageDTO.class);
        GalleryImageDTO galleryImageDTO1 = new GalleryImageDTO();
        galleryImageDTO1.setId(1L);
        GalleryImageDTO galleryImageDTO2 = new GalleryImageDTO();
        assertThat(galleryImageDTO1).isNotEqualTo(galleryImageDTO2);
        galleryImageDTO2.setId(galleryImageDTO1.getId());
        assertThat(galleryImageDTO1).isEqualTo(galleryImageDTO2);
        galleryImageDTO2.setId(2L);
        assertThat(galleryImageDTO1).isNotEqualTo(galleryImageDTO2);
        galleryImageDTO1.setId(null);
        assertThat(galleryImageDTO1).isNotEqualTo(galleryImageDTO2);
    }
}
