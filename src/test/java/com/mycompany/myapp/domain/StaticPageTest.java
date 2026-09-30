package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.StaticPageTestSamples.*;
import static com.mycompany.myapp.domain.StaticPageTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StaticPageTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(StaticPage.class);
        StaticPage staticPage1 = getStaticPageSample1();
        StaticPage staticPage2 = new StaticPage();
        assertThat(staticPage1).isNotEqualTo(staticPage2);

        staticPage2.setId(staticPage1.getId());
        assertThat(staticPage1).isEqualTo(staticPage2);

        staticPage2 = getStaticPageSample2();
        assertThat(staticPage1).isNotEqualTo(staticPage2);
    }

    @Test
    void childrenTest() {
        StaticPage staticPage = getStaticPageRandomSampleGenerator();
        StaticPage staticPageBack = getStaticPageRandomSampleGenerator();

        staticPage.addChildren(staticPageBack);
        assertThat(staticPage.getChildrens()).containsOnly(staticPageBack);
        assertThat(staticPageBack.getParent()).isEqualTo(staticPage);

        staticPage.removeChildren(staticPageBack);
        assertThat(staticPage.getChildrens()).doesNotContain(staticPageBack);
        assertThat(staticPageBack.getParent()).isNull();

        staticPage.childrens(new HashSet<>(Set.of(staticPageBack)));
        assertThat(staticPage.getChildrens()).containsOnly(staticPageBack);
        assertThat(staticPageBack.getParent()).isEqualTo(staticPage);

        staticPage.setChildrens(new HashSet<>());
        assertThat(staticPage.getChildrens()).doesNotContain(staticPageBack);
        assertThat(staticPageBack.getParent()).isNull();
    }

    @Test
    void parentTest() {
        StaticPage staticPage = getStaticPageRandomSampleGenerator();
        StaticPage staticPageBack = getStaticPageRandomSampleGenerator();

        staticPage.setParent(staticPageBack);
        assertThat(staticPage.getParent()).isEqualTo(staticPageBack);

        staticPage.parent(null);
        assertThat(staticPage.getParent()).isNull();
    }
}
