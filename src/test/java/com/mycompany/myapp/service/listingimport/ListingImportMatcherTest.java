package com.mycompany.myapp.service.listingimport;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.domain.ListingImportRow.MatchReason;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Chấm điểm nghi trùng (không cần Elasticsearch: kiểm tra trực tiếp {@link ListingImportMatcher#score}). */
class ListingImportMatcherTest {

    private static ListingImportMatcher.Normalized row(String name, String taxCode, String phone, String address) {
        Map<String, String> v = new HashMap<>();
        v.put("name", name);
        v.put("taxCode", taxCode);
        v.put("phone", phone);
        v.put("address", address);
        return ListingImportMatcher.Normalized.of(2, ImportRecord.of(v));
    }

    private static ListingImportMatcher.Candidate existing(String name, String taxCode, String phone, String address) {
        return new ListingImportMatcher.Candidate(100, name, taxCode, phone, null, address);
    }

    @Test
    void sameNameAndPhoneIsSuspect() {
        var m = ListingImportMatcher.score(
            row("Công ty TNHH Thương mại Ánh Dương", null, "0437685858", null),
            existing("CÔNG TY TNHH THƯƠNG MẠI ÁNH DƯƠNG", null, "(024) 3768 5858", "Hà Nội")
        );
        assertThat(m).isNotNull();
        assertThat(m.reason()).isEqualTo(MatchReason.NAME_PHONE);
        assertThat(m.listingId()).isEqualTo(100);
    }

    @Test
    void similarNameAndAddressIsSuspect() {
        var m = ListingImportMatcher.score(
            row("Ánh Dương Trading Co., Ltd", null, null, "Số 5 Lý Thường Kiệt, Hoàn Kiếm, Hà Nội"),
            existing("Công ty TNHH Ánh Dương Trading", null, null, "5 Lý Thường Kiệt, Phường Hoàn Kiếm, TP Hà Nội")
        );
        assertThat(m).isNotNull();
        assertThat(m.reason()).isEqualTo(MatchReason.NAME_ADDRESS);
    }

    @Test
    void identicalNameAloneIsSuspect() {
        var m = ListingImportMatcher.score(
            row("Công ty Cổ phần Ánh Dương Xanh", null, null, null),
            existing("ÁNH DƯƠNG XANH JSC", null, null, null)
        );
        assertThat(m).isNotNull();
        assertThat(m.reason()).isEqualTo(MatchReason.NAME);
    }

    @Test
    void abbreviatedNameWithSamePhoneIsSuspect() {
        // tên đầy đủ có thêm ngành ("xây dựng và thương mại") làm hệ số Dice thấp (0,44); tên ngắn nằm trọn trong tên dài + cùng SĐT
        var m = ListingImportMatcher.score(
            row("An Phước JSC", null, "0916 675 050", null),
            existing("Công TY CỔ PHẦN XÂY DỰNG VÀ THƯƠNG MẠI AN PHƯỚC", null, "0916675050", "Hà Nội")
        );
        assertThat(m).isNotNull();
        assertThat(m.reason()).isEqualTo(MatchReason.NAME_PHONE);
    }

    @Test
    void abbreviatedNameWithoutPhoneOrAddressIsNotSuspect() {
        var m = ListingImportMatcher.score(
            row("An Phước JSC", null, null, null),
            existing("Công ty CP Xây dựng và Thương mại An Phước", null, "0916675050", null)
        );
        assertThat(m).isNull();
    }

    @Test
    void differentTaxCodesAreNeverSuspect() {
        var m = ListingImportMatcher.score(
            row("Công ty TNHH Ánh Dương", "0101234567", "0912345678", null),
            existing("Công ty TNHH Ánh Dương", "0309876543", "0912345678", null)
        );
        assertThat(m).isNull();
    }

    @Test
    void sharedWordsButDifferentCompanyIsNotSuspect() {
        var m = ListingImportMatcher.score(
            row("Công ty TNHH Thương mại Dịch vụ Hoàng Long", null, "0912345678", "Đà Nẵng"),
            existing("Công ty TNHH Thương mại Dịch vụ Minh Phát", null, "0988777666", "Cần Thơ")
        );
        assertThat(m).isNull();
    }

    @Test
    void bestPicksHighestScore() {
        var r = row("Công ty TNHH Ánh Dương", null, "0912345678", null);
        var m = ListingImportMatcher.best(
            r,
            List.of(
                new ListingImportMatcher.Candidate(1, "Ánh Dương Xanh", null, null, null, null),
                new ListingImportMatcher.Candidate(2, "ÁNH DƯƠNG", null, "0912 345 678", null, null)
            )
        );
        assertThat(m).isNotNull();
        assertThat(m.listingId()).isEqualTo(2);
    }

    @Test
    void batchKeyUsesNamePlusPhone() {
        assertThat(row("Công ty TNHH Ánh Dương", null, "0912345678", null).batchKey()).isEqualTo(
            row("ÁNH DƯƠNG CO., LTD", null, "+84 912 345 678", null).batchKey()
        );
        assertThat(row("Công ty TNHH Ánh Dương", null, "0912345678", null).batchKey()).isNotEqualTo(
            row("Công ty TNHH Ánh Dương", null, "0988777666", null).batchKey()
        );
    }
}
