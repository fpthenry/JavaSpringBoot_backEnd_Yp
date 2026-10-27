package com.mycompany.myapp.service.listingimport;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ListingImportNormalizerTest {

    @Test
    void taxCodeKeepsValidFormats() {
        assertThat(ListingImportNormalizer.taxCode("0101234567")).isEqualTo("0101234567");
        assertThat(ListingImportNormalizer.taxCode(" 0101 234 567 ")).isEqualTo("0101234567");
        assertThat(ListingImportNormalizer.taxCode("0101234567-001")).isEqualTo("0101234567-001");
    }

    @Test
    void taxCodeRestoresLeadingZeroLostByExcel() {
        assertThat(ListingImportNormalizer.taxCode("101234567")).isEqualTo("0101234567");
        assertThat(ListingImportNormalizer.taxCode("101234567.0")).isEqualTo("0101234567");
        assertThat(ListingImportNormalizer.taxCode("101234567-001")).isEqualTo("0101234567-001");
    }

    @Test
    void taxCodeThirteenDigitsBecomesBranchFormat() {
        assertThat(ListingImportNormalizer.taxCode("0101234567001")).isEqualTo("0101234567-001");
    }

    @Test
    void taxCodeRejectsGarbage() {
        assertThat(ListingImportNormalizer.taxCode("MST-ABC")).isNull();
        assertThat(ListingImportNormalizer.isInvalidTaxCode("MST-ABC")).isTrue();
        assertThat(ListingImportNormalizer.taxCode("")).isNull();
        assertThat(ListingImportNormalizer.isInvalidTaxCode("  ")).isFalse();
    }

    @Test
    void phoneTakesFirstNumberDigitsOnly() {
        assertThat(ListingImportNormalizer.phone("(024) 3846.0171, 0912 345 678")).isEqualTo("02438460171");
        assertThat(ListingImportNormalizer.phone("+84 912 345 678")).isEqualTo("0912345678");
        assertThat(ListingImportNormalizer.phone("912345678")).isEqualTo("0912345678");
        assertThat(ListingImportNormalizer.phone("12345")).isNull();
    }

    @Test
    void samePhoneComparesLastEightDigits() {
        // mã vùng cũ 04 → 024, hoặc thiếu mã vùng
        assertThat(ListingImportNormalizer.samePhone("0437685858", "02437685858")).isTrue();
        assertThat(ListingImportNormalizer.samePhone("37685858", "02437685858")).isTrue();
        assertThat(ListingImportNormalizer.samePhone("0912345678", "0912345679")).isFalse();
        assertThat(ListingImportNormalizer.samePhone(null, "0912345678")).isFalse();
    }

    @Test
    void nameTokensDropLegalForms() {
        assertThat(ListingImportNormalizer.nameTokens("CÔNG TY TNHH THƯƠNG MẠI ÁNH DƯƠNG")).containsExactly(
            "thuong",
            "mai",
            "anh",
            "duong"
        );
        assertThat(ListingImportNormalizer.nameTokens("Công ty Cổ phần Ánh Dương")).containsExactly("anh", "duong");
        assertThat(ListingImportNormalizer.nameTokens("Anh Duong JSC")).containsExactly("anh", "duong");
        assertThat(ListingImportNormalizer.nameTokens("Công ty TNHH MTV Ánh Dương")).containsExactly("anh", "duong");
        assertThat(ListingImportNormalizer.nameKey("Công ty TNHH Ánh Dương")).isEqualTo(
            ListingImportNormalizer.nameKey("ÁNH DƯƠNG CO., LTD")
        );
    }

    @Test
    void diceSimilarity() {
        assertThat(ListingImportNormalizer.dice(java.util.Set.of("a", "b"), java.util.Set.of("a", "b"))).isEqualTo(1.0);
        assertThat(ListingImportNormalizer.dice(java.util.Set.of("a", "b"), java.util.Set.of("c"))).isZero();
        assertThat(ListingImportNormalizer.dice(java.util.Set.of("a", "b", "c", "d"), java.util.Set.of("a", "b", "c"))).isCloseTo(
            0.857,
            org.assertj.core.data.Offset.offset(0.001)
        );
    }

    @Test
    void containmentOfShorterName() {
        assertThat(
            ListingImportNormalizer.containment(java.util.Set.of("an", "phuoc"), java.util.Set.of("xay", "dung", "an", "phuoc"))
        ).isEqualTo(1.0);
        assertThat(ListingImportNormalizer.containment(java.util.Set.of("an"), java.util.Set.of("an", "phuoc"))).isZero(); // 1 từ: quá chung
    }

    @Test
    void addressTokensDropCommonWords() {
        assertThat(ListingImportNormalizer.addressTokens("Số 1 Tràng Tiền, Phường Hoàn Kiếm, TP Hà Nội")).containsExactly(
            "1",
            "trang",
            "tien",
            "hoan",
            "kiem",
            "ha",
            "noi"
        );
    }

    @Test
    void slugFromName() {
        assertThat(ListingImportNormalizer.slug("Công ty TNHH Á Châu & Co.")).isEqualTo("cong-ty-tnhh-a-chau-co");
        assertThat(ListingImportNormalizer.slug("   ")).isEqualTo("doanh-nghiep");
    }

    @Test
    void accentedNameWordsKeepDiacriticsDropLegalForms() {
        // Từ phân biệt giữ dấu để tìm trên field name (index chưa có name.folded)
        assertThat(ListingImportNormalizer.accentedNameWords("Công ty TNHH Tân Minh Technology")).isEqualTo("tân minh technology");
        assertThat(ListingImportNormalizer.accentedNameWords("CÔNG TY CỔ PHẦN ÁNH DƯƠNG")).isEqualTo("ánh dương");
        assertThat(ListingImportNormalizer.accentedNameWords("  ")).isEmpty();
        assertThat(ListingImportNormalizer.accentedNameWords(null)).isEmpty();
    }

    @Test
    void coordinateKeepsDigitsExactly() {
        // Giữ nguyên chữ số (không làm tròn), chỉ đổi dấu phân cách thành ":" như location_json
        assertThat(ListingImportNormalizer.coordinate("21.0245123,105.8562", null, null).value()).isEqualTo("21.0245123:105.8562");
        assertThat(ListingImportNormalizer.coordinate(" 21.0001000 : 105.698 ", null, null).value()).isEqualTo("21.0001000:105.698");
        assertThat(ListingImportNormalizer.coordinate("(10.7705 106.682)", null, null).value()).isEqualTo("10.7705:106.682");
        assertThat(ListingImportNormalizer.coordinate(null, "16.0544", "108.2022").value()).isEqualTo("16.0544:108.2022");
        assertThat(ListingImportNormalizer.coordinate(null, null, null)).isEqualTo(new ListingImportNormalizer.Coordinate(null, null));
    }

    @Test
    void coordinateRejectsOutsideVietnamAndGarbage() {
        // Đảo vĩ độ/kinh độ → ngoài Việt Nam → bỏ, có ghi chú
        ListingImportNormalizer.Coordinate swapped = ListingImportNormalizer.coordinate("105.8562,21.0245", null, null);
        assertThat(swapped.value()).isNull();
        assertThat(swapped.problem()).contains("ngoài Việt Nam");
        assertThat(ListingImportNormalizer.coordinate("21,0245", null, null).value()).isNull(); // dấu phẩy thập phân: không đoán
        assertThat(ListingImportNormalizer.coordinate("abc", null, null).problem()).isNotNull();
        assertThat(ListingImportNormalizer.coordinate(null, "21.02", null).problem()).contains("Thiếu");
    }

    @Test
    void logoMustBeHttpUrl() {
        assertThat(ListingImportNormalizer.httpUrl(" https://vidu.vn/logo.png ")).isEqualTo("https://vidu.vn/logo.png");
        assertThat(ListingImportNormalizer.httpUrl("logo.png")).isNull();
        assertThat(ListingImportNormalizer.httpUrl("javascript:alert(1)")).isNull();
    }

    @Test
    void imageUrlsFromListOrJson() {
        assertThat(
            ListingImportNormalizer.imageUrls("https://a.vn/1.jpg; https://a.vn/2.jpg | https://a.vn/1.jpg\nhttps://a.vn/3.jpg")
        ).containsExactly("https://a.vn/1.jpg", "https://a.vn/2.jpg", "https://a.vn/3.jpg");
        assertThat(
            ListingImportNormalizer.imageUrls("[{\"image_url\":\"https:\\/\\/a.vn\\/4.jpg\"},\"https://a.vn/5.jpg\"]")
        ).containsExactly("https://a.vn/4.jpg", "https://a.vn/5.jpg");
        assertThat(ListingImportNormalizer.imageUrls("anh-khong-phai-url.jpg")).isEmpty();
        assertThat(ListingImportNormalizer.imageUrls(null)).isEmpty();
        String many = String.join(
            ";",
            java.util.stream.IntStream.range(0, 40)
                .mapToObj(i -> "https://a.vn/" + i + ".jpg")
                .toList()
        );
        assertThat(ListingImportNormalizer.imageUrls(many)).hasSize(ListingImportNormalizer.MAX_IMAGES);
    }
}
