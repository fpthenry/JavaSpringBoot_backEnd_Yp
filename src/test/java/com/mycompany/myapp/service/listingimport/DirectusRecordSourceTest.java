package com.mycompany.myapp.service.listingimport;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.service.listingimport.DirectusRecordSource.ListingState;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class DirectusRecordSourceTest {

    private static final String ITEM = """
    {"id": 2009352, "status": "published", "name": "CÔNG TY TNHH AN PHÁT", "name_en": null, "address": "Xã Hưng Đạo, TP Hà Nội",
     "telephone": "0862695596", "taxcode": "0111437677", "province": "01", "ward": "09931", "industry_main": "5320",
     "slug": "cong-ty-tnhh-an-phat-0111437677", "begin": "2026-03-30", "managed": "Thuế cơ sở 22", "logo": "78d6198f-18e2",
     "content": null, "industry": [{"industries_code": "4610"}, {"industries_code": "4652"}]}
    """;

    private static final Instant OLD = Instant.parse("2023-07-19T00:00:00Z");
    private static final Instant NEW = Instant.parse("2025-10-30T00:00:00Z");

    private static ListingState state(Set<String> blank, boolean hasCategory, boolean hasWard) {
        return new ListingState(blank, hasCategory, true, hasWard, null, null);
    }

    private static ListingState current(String taxCode, Instant changedAt) {
        return new ListingState(Set.of(), true, true, true, taxCode, changedAt);
    }

    private final JsonNode item = new ObjectMapper().readTree(ITEM);

    @Test
    void mapsApiFieldsToImportRecord() {
        ImportRecord r = DirectusRecordSource.toRecord(item);

        assertThat(r.sourceRef()).isEqualTo("2009352");
        assertThat(r.name()).isEqualTo("CÔNG TY TNHH AN PHÁT");
        assertThat(r.taxCode()).isEqualTo("0111437677");
        assertThat(r.phone()).isEqualTo("0862695596");
        assertThat(r.province()).isEqualTo("01");
        assertThat(r.ward()).isEqualTo("09931");
        assertThat(r.industryCode()).isEqualTo("5320");
        assertThat(r.categories()).isEqualTo("4610;4652");
        assertThat(r.foundedYear()).isEqualTo("2026-03-30");
        assertThat(r.managedBy()).isEqualTo("Thuế cơ sở 22");
    }

    @Test
    void doesNotTakeDirectusFileIdAsLogo() {
        assertThat(DirectusRecordSource.toRecord(item).logo()).isNull();
    }

    @Test
    void listingWithNothingToFillNeedsNoWork() {
        assertThat(DirectusRecordSource.hasWork(item, state(Set.of(), true, true))).isFalse();
    }

    @Test
    void blankColumnWithoutApiValueNeedsNoWork() {
        assertThat(DirectusRecordSource.hasWork(item, state(Set.of("email", "description"), true, true))).isFalse();
    }

    @Test
    void blankColumnWithApiValueNeedsWork() {
        assertThat(DirectusRecordSource.hasWork(item, state(Set.of("phone"), true, true))).isTrue();
    }

    @Test
    void missingCategoryOrWardNeedsWork() {
        assertThat(DirectusRecordSource.hasWork(item, state(Set.of(), false, true))).isTrue();
        assertThat(DirectusRecordSource.hasWork(item, state(Set.of(), true, false))).isTrue();
    }

    @Test
    void newerSourceWithSameTaxCodeRefreshes() {
        assertThat(DirectusRecordSource.refreshes(item, NEW, current("0111437677", OLD))).isTrue();
        assertThat(DirectusRecordSource.refreshes(item, NEW, current(null, OLD))).isTrue();
    }

    @Test
    void olderOrUndatedSourceNeverRefreshes() {
        assertThat(DirectusRecordSource.refreshes(item, OLD, current("0111437677", NEW))).isFalse();
        assertThat(DirectusRecordSource.refreshes(item, null, current("0111437677", OLD))).isFalse();
    }

    @Test
    void differentTaxCodeNeverRefreshes() {
        assertThat(DirectusRecordSource.refreshes(item, NEW, current("0999999999", OLD))).isFalse();
    }

    @Test
    void spacesAndCaseAloneAreNotAChange() {
        assertThat(ListingImportWriter.differs("CÔNG TY  TNHH AN PHÁT", " Công ty TNHH An Phát ")).isFalse();
        assertThat(ListingImportWriter.differs("CÔNG TY TNHH AN PHÁT", "CÔNG TY TNHH AN BÌNH")).isTrue();
        assertThat(ListingImportWriter.differs(null, "CÔNG TY TNHH AN BÌNH")).isFalse();
    }

    @Test
    void sourceThatIsOnlyTheStartOfCurrentValueIsNotAChange() {
        assertThat(ListingImportWriter.differs("LÊ BÁ HÀ", "LÊ BÁ HÀ - Giới tính: Nam - Chức danh: Giám đốc")).isFalse();
        assertThat(ListingImportWriter.differs("Đang hoạt động", "Đang hoạt động (đã được cấp GCN ĐKT)")).isFalse();
        assertThat(ListingImportWriter.differs("abc@gmail.com", "abc@gmail.c")).isTrue();
    }

    @Test
    void dropsInternalYpIndustryCodes() {
        JsonNode mixed = new ObjectMapper().readTree(
            """
            {"industry": [{"industries_code": "113YP"}, {"industries_code": "6201"}, {"industries_code": "2490YP"}]}
            """
        );

        assertThat(DirectusRecordSource.categories(mixed)).isEqualTo("6201");
    }

    @Test
    void onlyInternalCodesMeansNoCategories() {
        JsonNode internal = new ObjectMapper().readTree(
            """
            {"industry": [{"industries_code": "113YP"}]}
            """
        );

        assertThat(DirectusRecordSource.categories(internal)).isNull();
    }

    @Test
    void parsesSourceTimeAndRejectsGarbage() {
        assertThat(DirectusPullService.sourceTime("2026-03-31T11:52:03")).isNotNull();
        assertThat(DirectusPullService.sourceTime("khong phai ngay")).isNull();
        assertThat(DirectusPullService.sourceTime(null)).isNull();
    }

    @Test
    void incrementalFilterAsksForPublishedRecordsChangedAfterTheMark() {
        String full = DirectusPullService.filter(0, null);
        String changed = DirectusPullService.filter(2009352, "2026-11-05T00:00:00");

        assertThat(new ObjectMapper().readTree(full).path("_and")).hasSize(2);
        assertThat(changed)
            .contains("\"_gt\":2009352")
            .contains("\"date_updated\":{\"_gt\":\"2026-11-05T00:00:00\"}")
            .contains("\"source_last_updated\":{\"_gt\":\"2026-11-05T00:00:00\"}");
        assertThat(new ObjectMapper().readTree(changed).path("_and")).hasSize(3);
    }

    @Test
    void hashIsStableAndChangesWithContent() {
        assertThat(DirectusPullService.sha256("a")).isEqualTo(DirectusPullService.sha256("a")).hasSize(64);
        assertThat(DirectusPullService.sha256("a")).isNotEqualTo(DirectusPullService.sha256("b"));
    }
}
