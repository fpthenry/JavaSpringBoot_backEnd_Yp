package com.mycompany.myapp.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ListingImageDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ListingImageDTO.class);
        ListingImageDTO listingImageDTO1 = new ListingImageDTO();
        listingImageDTO1.setId(1L);
        ListingImageDTO listingImageDTO2 = new ListingImageDTO();
        assertThat(listingImageDTO1).isNotEqualTo(listingImageDTO2);
        listingImageDTO2.setId(listingImageDTO1.getId());
        assertThat(listingImageDTO1).isEqualTo(listingImageDTO2);
        listingImageDTO2.setId(2L);
        assertThat(listingImageDTO1).isNotEqualTo(listingImageDTO2);
        listingImageDTO1.setId(null);
        assertThat(listingImageDTO1).isNotEqualTo(listingImageDTO2);
    }
}
