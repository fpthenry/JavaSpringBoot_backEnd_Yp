package com.mycompany.myapp.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BlogContentTest {

    @Test
    void cleanHtmlBoShortcodeGiuNoiDungBenTrong() {
        String html =
            "[vc_row][vc_column][vc_column_text css=\".x\"]<p>Đoạn <b>một</b></p><p><img src=\"https://a/b.jpg\"></p>" +
            "[/vc_column_text][vc_single_image image=\"12\"][/vc_column][/vc_row]<p> </p>";
        assertThat(BlogContent.cleanHtml(html)).isEqualTo("<p>Đoạn <b>một</b></p><p><img src=\"https://a/b.jpg\"></p>");
        assertThat(BlogContent.cleanHtml("<p>[ITALY] giữ nguyên</p>")).isEqualTo("<p>[ITALY] giữ nguyên</p>");
        assertThat(BlogContent.cleanHtml(null)).isNull();
    }

    @Test
    void plainTextBoTheLinkXemBaiVietVaGiaiMaKyTu() {
        String excerpt =
            "<p>Ngành chăn nuôi &#8220;tâm lý học&#8221;&nbsp;gia cầm&#8230; " +
            "<a class=\"view-article\" href=\"https://yp.com.vn/bai-cu/\">Xem bài viết</a></p>\n";
        assertThat(BlogContent.plainText(excerpt, 300)).isEqualTo("Ngành chăn nuôi “tâm lý học” gia cầm…");
    }

    @Test
    void plainTextCatORanhGioiTuVaBoScriptStyleShortcode() {
        String html = "<style>p{}</style><script>alert(1)</script>[vc_row]<p>một hai ba bốn năm</p>[/vc_row]";
        assertThat(BlogContent.plainText(html, 100)).isEqualTo("một hai ba bốn năm");
        assertThat(BlogContent.plainText(html, 12)).isEqualTo("một hai ba…");
        assertThat(BlogContent.plainText(null, 10)).isNull();
    }
}
