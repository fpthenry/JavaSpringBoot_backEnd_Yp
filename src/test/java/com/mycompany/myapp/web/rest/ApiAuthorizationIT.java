package com.mycompany.myapp.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mycompany.myapp.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Sửa tay (rà soát bảo mật 2026-10-27): tài khoản thường (ROLE_USER, vd tài khoản mặc định user/user) KHÔNG được
 * đọc/ghi API dữ liệu. Trước đây "/api/**" chỉ cần đăng nhập nên ROLE_USER xóa được doanh nghiệp, bài viết, banner…
 */
@IntegrationTest
@AutoConfigureMockMvc
class ApiAuthorizationIT {

    @Autowired
    private MockMvc mvc;

    @Test
    @WithMockUser
    void normalUserCannotWriteData() throws Exception {
        mvc.perform(delete("/api/listings/{id}", Long.MAX_VALUE)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/blog-posts/{id}", Long.MAX_VALUE)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/categories/{id}", Long.MAX_VALUE)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/locations/{id}", Long.MAX_VALUE)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/tags/{id}", Long.MAX_VALUE)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/galleries/{id}", Long.MAX_VALUE)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/blog-categories/{id}", Long.MAX_VALUE)).andExpect(status().isForbidden());
        mvc.perform(post("/api/gallery-images").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\"}")).andExpect(
            status().isForbidden()
        );
    }

    @Test
    @WithMockUser
    void normalUserCannotReadAdminData() throws Exception {
        mvc.perform(get("/api/listings")).andExpect(status().isForbidden());
        mvc.perform(get("/api/users")).andExpect(status().isForbidden());
    }

    @Test
    void anonymousIsUnauthorized() throws Exception {
        mvc.perform(get("/api/listings")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/listings/{id}", Long.MAX_VALUE)).andExpect(status().isUnauthorized());
    }
}
