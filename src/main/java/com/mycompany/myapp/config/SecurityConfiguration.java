package com.mycompany.myapp.config;

import static org.springframework.security.config.Customizer.withDefaults;

import com.mycompany.myapp.security.*;
import com.mycompany.myapp.web.filter.SpaWebFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import tech.jhipster.config.JHipsterProperties;

@Configuration
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfiguration {

    private final JHipsterProperties jHipsterProperties;

    public SecurityConfiguration(JHipsterProperties jHipsterProperties) {
        this.jHipsterProperties = jHipsterProperties;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http.cors(withDefaults())
            .csrf(csrf -> csrf.disable())
            .addFilterAfter(new SpaWebFilter(), BasicAuthenticationFilter.class)
            .headers(headers ->
                headers
                    .contentSecurityPolicy(csp -> csp.policyDirectives(jHipsterProperties.getSecurity().getContentSecurityPolicy()))
                    .frameOptions(FrameOptionsConfig::sameOrigin)
                    // Sửa tay: không gửi Referer — nhiều website chặn ảnh bị nhúng từ trang khác khi có Referer (dantri trả 403),
                    // ảnh trong bài viết copy từ các trang đó mới hiện được; đồng thời không lộ địa chỉ trang quản trị ra ngoài.
                    .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                    .permissionsPolicyHeader(permissions ->
                        permissions.policy(
                            "camera=(), fullscreen=(self), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), midi=(), payment=(), sync-xhr=()"
                        )
                    )
            )
            .authorizeHttpRequests(authz ->
                // prettier-ignore
                authz
                    .requestMatchers("/index.html", "/*.js", "/*.txt", "/*.json", "/*.map", "/*.css").permitAll()
                    .requestMatchers("/*.ico", "/*.png", "/*.svg", "/*.webapp").permitAll()
                    .requestMatchers("/content/**").permitAll()
                    .requestMatchers("/resources/**").permitAll()
                    .requestMatchers("/swagger-ui/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/authenticate").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/authenticate").permitAll()
                    .requestMatchers("/api/register").permitAll()
                    .requestMatchers("/api/activate").permitAll()
                    .requestMatchers("/api/account/reset-password/init").permitAll()
                    .requestMatchers("/api/account/reset-password/finish").permitAll()
                    // Sửa tay (rà soát bảo mật 2026-10-27): trước đây "/api/**" chỉ cần đăng nhập → tài khoản thường
                    // (vd user/user) thêm/sửa/xóa được doanh nghiệp, bài viết, banner… Nay chỉ tài khoản của chính mình
                    // (xem, sửa thông tin, đổi mật khẩu) cho người đã đăng nhập; mọi API dữ liệu cần ROLE_ADMIN.
                    .requestMatchers("/api/account", "/api/account/**").authenticated()
                    // Sửa tay (2026-11-17): quyền theo từng mục (AuthoritiesConstants). GET cần quyền xem hoặc quản lý của mục đó;
                    // thêm/sửa/xóa cần quyền quản lý. Dữ liệu tra cứu dùng chung (ngành nghề, địa giới, chuyên mục, tag, danh sách
                    // user) cho mọi nhân viên đọc, vì form doanh nghiệp/bài viết cần chọn từ đó. Các API còn lại vẫn chỉ ROLE_ADMIN.
                    .requestMatchers(HttpMethod.GET, "/api/listings/**")
                        .hasAnyAuthority(AuthoritiesConstants.ADMIN, AuthoritiesConstants.LISTING_VIEW, AuthoritiesConstants.LISTING_EDIT)
                    .requestMatchers("/api/listings/**").hasAnyAuthority(AuthoritiesConstants.ADMIN, AuthoritiesConstants.LISTING_EDIT)
                    .requestMatchers(HttpMethod.GET, "/api/categories/**", "/api/locations/**", "/api/category-index/**", "/api/location-index/**")
                        .hasAnyAuthority(AuthoritiesConstants.STAFF)
                    .requestMatchers("/api/categories/**", "/api/locations/**")
                        .hasAnyAuthority(AuthoritiesConstants.ADMIN, AuthoritiesConstants.CATALOG_EDIT)
                    .requestMatchers(HttpMethod.GET, "/api/blog-posts/**")
                        .hasAnyAuthority(AuthoritiesConstants.ADMIN, AuthoritiesConstants.POST_VIEW, AuthoritiesConstants.POST_EDIT)
                    .requestMatchers(HttpMethod.GET, "/api/blog-categories/**", "/api/tags/**", "/api/users/**")
                        .hasAnyAuthority(AuthoritiesConstants.STAFF)
                    .requestMatchers("/api/blog-posts/**", "/api/blog-categories/**", "/api/tags/**")
                        .hasAnyAuthority(AuthoritiesConstants.ADMIN, AuthoritiesConstants.POST_EDIT)
                    .requestMatchers(HttpMethod.GET, "/api/galleries/**", "/api/gallery-images/**")
                        .hasAnyAuthority(AuthoritiesConstants.ADMIN, AuthoritiesConstants.BANNER_VIEW, AuthoritiesConstants.BANNER_EDIT)
                    .requestMatchers("/api/galleries/**", "/api/gallery-images/**")
                        .hasAnyAuthority(AuthoritiesConstants.ADMIN, AuthoritiesConstants.BANNER_EDIT)
                    .requestMatchers("/api/**").hasAuthority(AuthoritiesConstants.ADMIN)
                    .requestMatchers("/v3/api-docs/**").hasAuthority(AuthoritiesConstants.ADMIN)
                    .requestMatchers("/management/health").permitAll()
                    .requestMatchers("/management/health/**").permitAll()
                    .requestMatchers("/management/info").permitAll()
                    .requestMatchers("/management/prometheus").permitAll()
                    .requestMatchers("/management/**").hasAuthority(AuthoritiesConstants.ADMIN)
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions ->
                exceptions
                    .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                    .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()));
        return http.build();
    }
}
