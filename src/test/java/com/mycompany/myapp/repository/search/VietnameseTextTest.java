package com.mycompany.myapp.repository.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VietnameseTextTest {

    @Test
    void ghepDauToHopThanhKyTuDungSan() {
        // "Hà Nội" gõ kiểu Unicode tổ hợp: a + dấu huyền, ô + dấu nặng
        String decomposed = "Ha\u0300 No\u0302\u0323i";
        assertThat(VietnameseText.normalize(decomposed)).isEqualTo("Hà Nội");
    }

    @Test
    void doiKieuDatDauCuSangKieuMoiChoVanMo() {
        assertThat(VietnameseText.normalize("hoà bình, thuỷ lợi, uỷ ban, khoẻ, HOÀ, Thuý")).isEqualTo(
            "hòa bình, thủy lợi, ủy ban, khỏe, HÒA, Thúy"
        );
    }

    @Test
    void giuNguyenKhiKhongPhaiVanMoHoacLaQuy() {
        assertThat(VietnameseText.normalize("hoàn thành, xoáy, quý, quà, khuyến mại, huỳnh, hòa")).isEqualTo(
            "hoàn thành, xoáy, quý, quà, khuyến mại, huỳnh, hòa"
        );
        assertThat(VietnameseText.normalize(null)).isNull();
    }

    @Test
    void boDauVaNhanBietTuCoDau() {
        assertThat(VietnameseText.fold("Đá Hà Nội")).isEqualTo("Da Ha Noi");
        assertThat(VietnameseText.hasDiacritics("đá")).isTrue();
        assertThat(VietnameseText.hasDiacritics("đ")).isTrue();
        assertThat(VietnameseText.hasDiacritics("HA")).isFalse();
    }

    @Test
    void tachTuBoDauCau() {
        assertThat(VietnameseText.words(" Covid-19: Hà  Nội! ")).containsExactly("Covid", "19", "Hà", "Nội");
        assertThat(VietnameseText.words("--")).isEmpty();
    }
}
