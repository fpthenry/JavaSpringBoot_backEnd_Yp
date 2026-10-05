package com.mycompany.myapp.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class PublicApiKeyFilterTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private Authentication filter(List<String> keys, String header) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/public/v1/galleries");
        if (header != null) {
            request.addHeader(PublicApiKeyFilter.HEADER, header);
        }
        MockFilterChain chain = new MockFilterChain();
        new PublicApiKeyFilter(keys).doFilter(request, new MockHttpServletResponse(), chain);
        assertThat(chain.getRequest()).as("request luôn được chuyển tiếp").isNotNull();
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    void validKeyGrantsPublicApiAuthority() throws Exception {
        Authentication auth = filter(List.of("key-a", "key-b"), "key-b");
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly(AuthoritiesConstants.PUBLIC_API);
    }

    @Test
    void keyIsTrimmed() throws Exception {
        assertThat(filter(List.of(" key-a "), "key-a ")).isNotNull();
    }

    @Test
    void missingOrWrongKeyLeavesRequestAnonymous() throws Exception {
        assertThat(filter(List.of("key-a"), null)).isNull();
        assertThat(filter(List.of("key-a"), "key-x")).isNull();
        assertThat(filter(List.of("key-a"), "")).isNull();
    }

    @Test
    void noConfiguredKeysRejectsEverything() throws Exception {
        PublicApiKeyFilter emptyFilter = new PublicApiKeyFilter(Arrays.asList("", "  ", null));
        assertThat(emptyFilter.hasKeys()).isFalse();
        assertThat(filter(Arrays.asList("", "  ", null), "")).isNull();
    }
}
