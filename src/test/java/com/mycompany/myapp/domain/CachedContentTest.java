package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.CachedContentTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class CachedContentTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(CachedContent.class);
        CachedContent cachedContent1 = getCachedContentSample1();
        CachedContent cachedContent2 = new CachedContent();
        assertThat(cachedContent1).isNotEqualTo(cachedContent2);

        cachedContent2.setId(cachedContent1.getId());
        assertThat(cachedContent1).isEqualTo(cachedContent2);

        cachedContent2 = getCachedContentSample2();
        assertThat(cachedContent1).isNotEqualTo(cachedContent2);
    }
}
