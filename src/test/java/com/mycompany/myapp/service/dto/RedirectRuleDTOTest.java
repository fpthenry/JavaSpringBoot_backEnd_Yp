package com.mycompany.myapp.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class RedirectRuleDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(RedirectRuleDTO.class);
        RedirectRuleDTO redirectRuleDTO1 = new RedirectRuleDTO();
        redirectRuleDTO1.setId(1L);
        RedirectRuleDTO redirectRuleDTO2 = new RedirectRuleDTO();
        assertThat(redirectRuleDTO1).isNotEqualTo(redirectRuleDTO2);
        redirectRuleDTO2.setId(redirectRuleDTO1.getId());
        assertThat(redirectRuleDTO1).isEqualTo(redirectRuleDTO2);
        redirectRuleDTO2.setId(2L);
        assertThat(redirectRuleDTO1).isNotEqualTo(redirectRuleDTO2);
        redirectRuleDTO1.setId(null);
        assertThat(redirectRuleDTO1).isNotEqualTo(redirectRuleDTO2);
    }
}
