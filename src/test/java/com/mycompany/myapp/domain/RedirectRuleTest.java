package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.RedirectRuleTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class RedirectRuleTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(RedirectRule.class);
        RedirectRule redirectRule1 = getRedirectRuleSample1();
        RedirectRule redirectRule2 = new RedirectRule();
        assertThat(redirectRule1).isNotEqualTo(redirectRule2);

        redirectRule2.setId(redirectRule1.getId());
        assertThat(redirectRule1).isEqualTo(redirectRule2);

        redirectRule2 = getRedirectRuleSample2();
        assertThat(redirectRule1).isNotEqualTo(redirectRule2);
    }
}
