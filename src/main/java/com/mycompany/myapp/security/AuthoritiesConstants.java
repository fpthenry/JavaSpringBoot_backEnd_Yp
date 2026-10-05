package com.mycompany.myapp.security;

/**
 * Constants for Spring Security authorities.
 */
public final class AuthoritiesConstants {

    public static final String ADMIN = "ROLE_ADMIN";

    public static final String USER = "ROLE_USER";

    public static final String ANONYMOUS = "ROLE_ANONYMOUS";

    /** Sửa tay: client của API công khai /api/public/** (FE Next.js) đã gửi đúng X-API-Key. */
    public static final String PUBLIC_API = "ROLE_PUBLIC_API";

    private AuthoritiesConstants() {}
}
