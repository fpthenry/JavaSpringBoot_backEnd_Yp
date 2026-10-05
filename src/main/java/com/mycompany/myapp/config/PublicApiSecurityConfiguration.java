package com.mycompany.myapp.config;

import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.security.PublicApiKeyFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Bảo mật riêng cho API công khai {@code /api/public/**} (FE Next.js), tách khỏi chuỗi JWT của quản trị viên
 * ({@link SecurityConfiguration}, không sửa file JHipster sinh ra).
 * <ul>
 *   <li>{@code /api/public/v1/media/**}: ảnh, ai cũng tải được (thẻ img trên trình duyệt không gửi được header).</li>
 *   <li>{@code GET /api/public/**} còn lại: phải có header {@code X-API-Key} đúng.</li>
 *   <li>Mọi phương thức khác: từ chối (API công khai chỉ đọc).</li>
 * </ul>
 * Chuỗi này chạy trước chuỗi mặc định ({@code @Order(1)}) và chỉ áp dụng cho {@code /api/public/**}.
 */
@Configuration
public class PublicApiSecurityConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(PublicApiSecurityConfiguration.class);

    public static final String PUBLIC_API_PATTERN = "/api/public/**";
    public static final String PUBLIC_MEDIA_PATTERN = "/api/public/v1/media/**";

    @Bean
    @Order(1)
    public SecurityFilterChain publicApiFilterChain(HttpSecurity http, ApplicationProperties applicationProperties) {
        PublicApiKeyFilter apiKeyFilter = new PublicApiKeyFilter(applicationProperties.getPublicApi().getKeys());
        if (!apiKeyFilter.hasKeys()) {
            LOG.warn("Chưa cấu hình application.public-api.keys: mọi request tới {} (trừ ảnh) sẽ bị từ chối", PUBLIC_API_PATTERN);
        }
        http.securityMatcher(PUBLIC_API_PATTERN)
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(cache -> cache.disable())
            .addFilterBefore(apiKeyFilter, AnonymousAuthenticationFilter.class)
            .authorizeHttpRequests(authz ->
                authz
                    .requestMatchers(HttpMethod.GET, PUBLIC_MEDIA_PATTERN)
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, PUBLIC_API_PATTERN)
                    .hasAuthority(AuthoritiesConstants.PUBLIC_API)
                    .anyRequest()
                    .denyAll()
            )
            // Đặt mã trạng thái trực tiếp (không sendError): sendError chuyển sang /error do chuỗi JWT xử lý, 403 thành 401
            .exceptionHandling(exceptions ->
                exceptions
                    .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                    .accessDeniedHandler((request, response, denied) -> response.setStatus(HttpStatus.FORBIDDEN.value()))
            );
        return http.build();
    }
}
