package com.mycompany.myapp.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class CachedContentDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(CachedContentDTO.class);
        CachedContentDTO cachedContentDTO1 = new CachedContentDTO();
        cachedContentDTO1.setId(1L);
        CachedContentDTO cachedContentDTO2 = new CachedContentDTO();
        assertThat(cachedContentDTO1).isNotEqualTo(cachedContentDTO2);
        cachedContentDTO2.setId(cachedContentDTO1.getId());
        assertThat(cachedContentDTO1).isEqualTo(cachedContentDTO2);
        cachedContentDTO2.setId(2L);
        assertThat(cachedContentDTO1).isNotEqualTo(cachedContentDTO2);
        cachedContentDTO1.setId(null);
        assertThat(cachedContentDTO1).isNotEqualTo(cachedContentDTO2);
    }
}
