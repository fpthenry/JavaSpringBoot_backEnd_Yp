package com.mycompany.myapp.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class BlogCategoryDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(BlogCategoryDTO.class);
        BlogCategoryDTO blogCategoryDTO1 = new BlogCategoryDTO();
        blogCategoryDTO1.setId(1L);
        BlogCategoryDTO blogCategoryDTO2 = new BlogCategoryDTO();
        assertThat(blogCategoryDTO1).isNotEqualTo(blogCategoryDTO2);
        blogCategoryDTO2.setId(blogCategoryDTO1.getId());
        assertThat(blogCategoryDTO1).isEqualTo(blogCategoryDTO2);
        blogCategoryDTO2.setId(2L);
        assertThat(blogCategoryDTO1).isNotEqualTo(blogCategoryDTO2);
        blogCategoryDTO1.setId(null);
        assertThat(blogCategoryDTO1).isNotEqualTo(blogCategoryDTO2);
    }
}
