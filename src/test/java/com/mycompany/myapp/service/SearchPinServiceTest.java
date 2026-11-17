package com.mycompany.myapp.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Sửa tay: khóa so khớp từ khóa ghim (không phân biệt dấu, hoa thường, dấu câu). */
class SearchPinServiceTest {

    @Test
    void keyIgnoresAccentsCaseAndPunctuation() {
        assertThat(SearchPinService.key("Máy nén khí")).isEqualTo("may nen khi");
        assertThat(SearchPinService.key("  may   nen khi ")).isEqualTo("may nen khi");
        assertThat(SearchPinService.key("MÁY NÉN-KHÍ")).isEqualTo("may nen khi");
        assertThat(SearchPinService.key("Đại Phong")).isEqualTo("dai phong");
        assertThat(SearchPinService.key("NƯỚC -THIẾT BỊ & DỊCH VỤ XỬ LÝ NƯỚC")).isEqualTo("nuoc thiet bi dich vu xu ly nuoc");
    }

    @Test
    void keyNormalizesOldTonePlacement() {
        // "hoà" (dấu kiểu cũ) và "hòa" là một
        assertThat(SearchPinService.key("hoà bình")).isEqualTo(SearchPinService.key("hòa bình"));
    }

    @Test
    void keyOfBlankIsEmpty() {
        assertThat(SearchPinService.key(null)).isEmpty();
        assertThat(SearchPinService.key("  - ")).isEmpty();
    }
}
