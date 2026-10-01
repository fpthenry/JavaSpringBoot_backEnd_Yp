package com.mycompany.myapp.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ListingDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ListingDTO.class);
        ListingDTO listingDTO1 = new ListingDTO();
        listingDTO1.setId(1L);
        ListingDTO listingDTO2 = new ListingDTO();
        assertThat(listingDTO1).isNotEqualTo(listingDTO2);
        listingDTO2.setId(listingDTO1.getId());
        assertThat(listingDTO1).isEqualTo(listingDTO2);
        listingDTO2.setId(2L);
        assertThat(listingDTO1).isNotEqualTo(listingDTO2);
        listingDTO1.setId(null);
        assertThat(listingDTO1).isNotEqualTo(listingDTO2);
    }
}
