package com.mycompany.myapp.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ArticleCategoryDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ArticleCategoryDTO.class);
        ArticleCategoryDTO articleCategoryDTO1 = new ArticleCategoryDTO();
        articleCategoryDTO1.setId(1L);
        ArticleCategoryDTO articleCategoryDTO2 = new ArticleCategoryDTO();
        assertThat(articleCategoryDTO1).isNotEqualTo(articleCategoryDTO2);
        articleCategoryDTO2.setId(articleCategoryDTO1.getId());
        assertThat(articleCategoryDTO1).isEqualTo(articleCategoryDTO2);
        articleCategoryDTO2.setId(2L);
        assertThat(articleCategoryDTO1).isNotEqualTo(articleCategoryDTO2);
        articleCategoryDTO1.setId(null);
        assertThat(articleCategoryDTO1).isNotEqualTo(articleCategoryDTO2);
    }
}
