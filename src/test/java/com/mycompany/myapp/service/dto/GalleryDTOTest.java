package com.mycompany.myapp.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class GalleryDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(GalleryDTO.class);
        GalleryDTO galleryDTO1 = new GalleryDTO();
        galleryDTO1.setId(1L);
        GalleryDTO galleryDTO2 = new GalleryDTO();
        assertThat(galleryDTO1).isNotEqualTo(galleryDTO2);
        galleryDTO2.setId(galleryDTO1.getId());
        assertThat(galleryDTO1).isEqualTo(galleryDTO2);
        galleryDTO2.setId(2L);
        assertThat(galleryDTO1).isNotEqualTo(galleryDTO2);
        galleryDTO1.setId(null);
        assertThat(galleryDTO1).isNotEqualTo(galleryDTO2);
    }
}
