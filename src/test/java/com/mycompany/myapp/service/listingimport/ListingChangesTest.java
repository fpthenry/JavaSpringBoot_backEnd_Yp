package com.mycompany.myapp.service.listingimport;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.domain.Category;
import com.mycompany.myapp.domain.Listing;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class ListingChangesTest {

    private static final Instant BEFORE = Instant.parse("2023-07-19T00:00:00Z");
    private static final Instant NOW = Instant.parse("2026-11-06T00:00:00Z");

    private static Listing listing() {
        Listing l = new Listing();
        l.setName("TÊN CŨ");
        l.setModifiedAt(BEFORE);
        l.setUpdatedAt(BEFORE);
        return l;
    }

    private static Category category(long id) {
        Category c = new Category();
        c.setId(id);
        return c;
    }

    @Test
    void separatesFilledFromOverwrittenFields() {
        Listing l = listing();
        ListingChanges.Snapshot before = ListingChanges.snapshot(l);
        l.setName("TÊN MỚI");
        l.setPhone("0901234567");

        ListingChanges changes = before.diff(l);

        assertThat(changes.fields()).extracting(ListingChanges.FieldChange::field).containsExactly("name", "phone");
        assertThat(changes.updatedFields()).containsExactly("name");
        assertThat(changes.modifiedAt()).isEqualTo(BEFORE);
    }

    @Test
    void locationJsonIsTrackedButNotReportedAsOverwritten() {
        Listing l = listing();
        l.setLocationJson("{\"ward\":null}");
        ListingChanges.Snapshot before = ListingChanges.snapshot(l);
        l.setLocationJson("{\"ward\":\"Phường A\"}");

        assertThat(before.diff(l).updatedFields()).isEmpty();
    }

    @Test
    void undoRestoresValuesLinksAndTimestamps() {
        Listing l = listing();
        l.addCategory(category(1));
        ListingChanges.Snapshot before = ListingChanges.snapshot(l);
        l.setName("TÊN MỚI");
        l.setPhone("0901234567");
        l.addCategory(category(2));
        l.setModifiedAt(NOW);
        l.setUpdatedAt(NOW);

        int skipped = before.diff(l).undo(l);

        assertThat(skipped).isZero();
        assertThat(l.getName()).isEqualTo("TÊN CŨ");
        assertThat(l.getPhone()).isNull();
        assertThat(l.getCategories()).extracting(Category::getId).containsExactly(1L);
        assertThat(l.getModifiedAt()).isEqualTo(BEFORE);
        assertThat(l.getUpdatedAt()).isEqualTo(BEFORE);
    }

    @Test
    void undoKeepsFieldChangedAfterTheImport() {
        Listing l = listing();
        ListingChanges.Snapshot before = ListingChanges.snapshot(l);
        l.setName("TÊN TỪ ĐỢT NHẬP");
        ListingChanges changes = before.diff(l);
        l.setName("ADMIN SỬA TAY");

        int skipped = changes.undo(l);

        assertThat(skipped).isEqualTo(1);
        assertThat(l.getName()).isEqualTo("ADMIN SỬA TAY");
    }

    @Test
    void readsHistoryRebuiltByMysqlJsonFunctions() {
        String json = """
        {"fields": [{"field": "address", "updated": true, "newValue": "Mới", "oldValue": "Cũ"}],
         "locations": [], "categories": [12], "modifiedAt": "2023-07-19T00:00:00.000000Z", "updatedAt": "2023-07-19T00:00:00.000000Z"}
        """;

        ListingChanges changes = new ObjectMapper().readValue(json, ListingChanges.class);

        assertThat(changes.updatedFields()).containsExactly("address");
        assertThat(changes.categories()).containsExactly(12L);
        assertThat(changes.modifiedAt()).isEqualTo(BEFORE);
    }

    @Test
    void survivesJsonRoundTrip() {
        Listing l = listing();
        ListingChanges.Snapshot before = ListingChanges.snapshot(l);
        l.setName("TÊN MỚI");
        l.addCategory(category(7));
        ObjectMapper mapper = new ObjectMapper();

        ListingChanges back = mapper.readValue(mapper.writeValueAsString(before.diff(l)), ListingChanges.class);

        assertThat(back).isEqualTo(before.diff(l));
    }
}
