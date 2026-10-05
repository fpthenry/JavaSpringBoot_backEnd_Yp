package com.mycompany.myapp.service.wordpress;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class WordPressPostSyncServiceTest {

    @Test
    void parsesWordPressGmtDates() {
        assertThat(WordPressPostSyncService.parseGmt("2026-10-05T07:09:50")).isEqualTo(Instant.parse("2026-10-05T07:09:50Z"));
        assertThat(WordPressPostSyncService.parseGmt("0000-00-00T00:00:00")).isNull();
        assertThat(WordPressPostSyncService.parseGmt(null)).isNull();
    }

    @Test
    void decodesHtmlEntitiesInTitles() {
        assertThat(WordPressPostSyncService.unescape("&#8220;Nh&agrave; M&aacute;y&#8221; &amp; Chuy&#7871;n")).isEqualTo(
            "“Nhà Máy” & Chuyến"
        );
    }

    @Test
    void decodesPercentEncodedSlugs() {
        assertThat(WordPressPostSyncService.decodeSlug("ha-n%e1%bb%99i")).isEqualTo("ha-nội");
        assertThat(WordPressPostSyncService.decodeSlug("cong-doan-vnpt")).isEqualTo("cong-doan-vnpt");
        assertThat(WordPressPostSyncService.decodeSlug("bad%zz")).isEqualTo("bad%zz");
    }

    @Test
    void truncatesLongValues() {
        assertThat(WordPressPostSyncService.truncate("abcdef", 3)).isEqualTo("abc");
        assertThat(WordPressPostSyncService.truncate("ab", 3)).isEqualTo("ab");
        assertThat(WordPressPostSyncService.truncate(null, 3)).isNull();
    }

    @Test
    void readsTextFieldsSafely() {
        JsonNode node = JsonMapper.builder().build().readTree("{\"a\":\"x\",\"b\":null,\"title\":{\"rendered\":\"T\"}}");
        assertThat(WordPressPostSyncService.text(node, "a")).isEqualTo("x");
        assertThat(WordPressPostSyncService.text(node, "b")).isNull();
        assertThat(WordPressPostSyncService.text(node, "missing")).isNull();
        assertThat(WordPressPostSyncService.text(node.path("title"), "rendered")).isEqualTo("T");
    }
}
