package com.mycompany.myapp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Xác thực client của API công khai bằng header {@value #HEADER}.
 * Khóa đúng thì đặt quyền {@link AuthoritiesConstants#PUBLIC_API}; sai hoặc thiếu thì để nguyên (ẩn danh)
 * và quy tắc phân quyền trả 401.
 */
public class PublicApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-API-Key";

    private final List<byte[]> keys;

    public PublicApiKeyFilter(List<String> keys) {
        this.keys = keys
            .stream()
            .filter(key -> key != null && !key.isBlank())
            .map(key -> key.trim().getBytes(StandardCharsets.UTF_8))
            .toList();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        if (provided != null && isValid(provided.trim().getBytes(StandardCharsets.UTF_8))) {
            var authentication = UsernamePasswordAuthenticationToken.authenticated(
                "public-api-client",
                null,
                List.of(new SimpleGrantedAuthority(AuthoritiesConstants.PUBLIC_API))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        chain.doFilter(request, response);
    }

    /** So sánh thời gian hằng (không lộ độ dài khớp qua thời gian phản hồi). */
    private boolean isValid(byte[] provided) {
        boolean valid = false;
        for (byte[] key : keys) {
            valid |= MessageDigest.isEqual(key, provided);
        }
        return valid;
    }

    public boolean hasKeys() {
        return !keys.isEmpty();
    }
}
