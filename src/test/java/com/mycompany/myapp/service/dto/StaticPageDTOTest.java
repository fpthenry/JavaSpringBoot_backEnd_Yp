package com.mycompany.myapp.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class StaticPageDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(StaticPageDTO.class);
        StaticPageDTO staticPageDTO1 = new StaticPageDTO();
        staticPageDTO1.setId(1L);
        StaticPageDTO staticPageDTO2 = new StaticPageDTO();
        assertThat(staticPageDTO1).isNotEqualTo(staticPageDTO2);
        staticPageDTO2.setId(staticPageDTO1.getId());
        assertThat(staticPageDTO1).isEqualTo(staticPageDTO2);
        staticPageDTO2.setId(2L);
        assertThat(staticPageDTO1).isNotEqualTo(staticPageDTO2);
        staticPageDTO1.setId(null);
        assertThat(staticPageDTO1).isNotEqualTo(staticPageDTO2);
    }
}
