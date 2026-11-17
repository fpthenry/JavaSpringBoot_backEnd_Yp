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

    /*
     * Sửa tay: quyền theo từng mục trong trang quản trị. Mỗi mục có 2 mức: _VIEW (chỉ xem) và _EDIT (thêm, sửa, xóa; đã gồm xem).
     * ROLE_ADMIN luôn có toàn quyền. Luật áp vào API ở SecurityConfiguration, menu và nút ở frontend (permissions.ts).
     */
    /** Doanh nghiệp: /api/listings. */
    public static final String LISTING_VIEW = "ROLE_LISTING_VIEW";
    public static final String LISTING_EDIT = "ROLE_LISTING_EDIT";
    /** Ngành nghề và địa giới: /api/categories, /api/locations, /api/category-index, /api/location-index. */
    public static final String CATALOG_VIEW = "ROLE_CATALOG_VIEW";
    public static final String CATALOG_EDIT = "ROLE_CATALOG_EDIT";
    /** Bài viết, chuyên mục, tag: /api/blog-posts, /api/blog-categories, /api/tags. */
    public static final String POST_VIEW = "ROLE_POST_VIEW";
    public static final String POST_EDIT = "ROLE_POST_EDIT";
    /** Banner (thư viện ảnh): /api/galleries, /api/gallery-images. */
    public static final String BANNER_VIEW = "ROLE_BANNER_VIEW";
    public static final String BANNER_EDIT = "ROLE_BANNER_EDIT";

    /** Mọi tài khoản nhân viên có ít nhất một quyền theo mục (kể cả admin): được đọc dữ liệu tra cứu dùng chung. */
    public static final String[] STAFF = {
        ADMIN,
        LISTING_VIEW,
        LISTING_EDIT,
        CATALOG_VIEW,
        CATALOG_EDIT,
        POST_VIEW,
        POST_EDIT,
        BANNER_VIEW,
        BANNER_EDIT,
    };

    private AuthoritiesConstants() {}
}
