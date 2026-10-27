package com.mycompany.myapp.security;

import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Sửa tay (rà soát bảo mật 2026-10-27): kiểu file ảnh upload được phép (banner — GalleryImage.image).
 * File ảnh upload được phục vụ công khai cùng tên miền với trang quản trị ({@code /api/public/v1/media/**}) và qua proxy
 * cùng tên miền FE. Nếu nhận kiểu do người gửi khai báo (text/html, image/svg+xml…) thì trình duyệt chạy được mã trong
 * file → đánh cắp token admin (stored XSS). Chỉ nhận ảnh raster; SVG bị từ chối vì chứa được script.
 */
public final class UploadedImageTypes {

    public static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp", "image/gif", "image/avif");

    private UploadedImageTypes() {}

    /** "image/PNG; charset=x" → "image/png"; "image/jpg" (tên không chuẩn hay gặp) → "image/jpeg"; null → null. */
    public static String normalize(String contentType) {
        if (contentType == null) {
            return null;
        }
        int semi = contentType.indexOf(';');
        String t = (semi >= 0 ? contentType.substring(0, semi) : contentType).trim().toLowerCase(Locale.ROOT);
        return "image/jpg".equals(t) ? "image/jpeg" : t;
    }

    public static boolean isAllowed(String contentType) {
        String t = normalize(contentType);
        return t != null && ALLOWED.contains(t);
    }

    /** Có file ảnh mà kiểu không được phép → 400. Không có file (chỉ dùng URL ảnh ngoài) thì bỏ qua. */
    public static void requireAllowed(byte[] image, String contentType) {
        if (image != null && image.length > 0 && !isAllowed(contentType)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Chỉ nhận ảnh JPEG, PNG, WebP, GIF, AVIF (không nhận " + (contentType == null ? "kiểu trống" : contentType) + ")"
            );
        }
    }
}
