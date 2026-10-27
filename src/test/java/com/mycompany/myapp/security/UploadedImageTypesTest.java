package com.mycompany.myapp.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class UploadedImageTypesTest {

    private static final byte[] DATA = { 1, 2, 3 };

    @Test
    void allowsRasterImagesOnly() {
        assertThat(UploadedImageTypes.isAllowed("image/png")).isTrue();
        assertThat(UploadedImageTypes.isAllowed("IMAGE/JPEG; charset=binary")).isTrue();
        assertThat(UploadedImageTypes.isAllowed("image/jpg")).isTrue(); // tên không chuẩn hay gặp
        assertThat(UploadedImageTypes.isAllowed("image/webp")).isTrue();
        // chạy được script trên trình duyệt → không nhận
        assertThat(UploadedImageTypes.isAllowed("image/svg+xml")).isFalse();
        assertThat(UploadedImageTypes.isAllowed("text/html")).isFalse();
        assertThat(UploadedImageTypes.isAllowed("application/octet-stream")).isFalse();
        assertThat(UploadedImageTypes.isAllowed(null)).isFalse();
    }

    @Test
    void requireAllowedRejectsDangerousUploadsButIgnoresMissingFile() {
        assertThatThrownBy(() -> UploadedImageTypes.requireAllowed(DATA, "text/html")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> UploadedImageTypes.requireAllowed(DATA, null)).isInstanceOf(ResponseStatusException.class);
        assertThatCode(() -> UploadedImageTypes.requireAllowed(DATA, "image/png")).doesNotThrowAnyException();
        // chỉ dùng URL ảnh ngoài, không upload file
        assertThatCode(() -> UploadedImageTypes.requireAllowed(null, null)).doesNotThrowAnyException();
    }
}
