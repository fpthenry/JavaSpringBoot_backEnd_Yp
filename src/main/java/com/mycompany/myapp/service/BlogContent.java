package com.mycompany.myapp.service;

import java.util.regex.Pattern;
import org.springframework.web.util.HtmlUtils;

/**
 * Làm sạch nội dung bài viết đồng bộ từ WordPress trước khi trả cho FE.
 */
public final class BlogContent {

    /** Shortcode WPBakery ({@code [vc_row]}, {@code [/vc_column_text]}...), kể cả bị cắt cụt không có {@code ]}. */
    private static final Pattern WP_SHORTCODE = Pattern.compile("\\[/?vc_[^\\]\\[]*\\]?");
    /** Link "Xem bài viết" WordPress tự thêm cuối tóm tắt, trỏ về site cũ. */
    private static final Pattern READ_MORE_LINK = Pattern.compile("<a\\s[^>]*class=\"view-article\"[^>]*>.*?</a>", Pattern.DOTALL);
    private static final Pattern SCRIPT_STYLE = Pattern.compile(
        "<(script|style)\\b[^>]*>.*?</\\1>",
        Pattern.DOTALL | Pattern.CASE_INSENSITIVE
    );
    private static final Pattern TAG = Pattern.compile("<[^>]*>");
    private static final Pattern SPACES = Pattern.compile("[\\s\\u00a0]+");
    /** Đoạn văn rỗng còn lại sau khi bỏ shortcode. */
    private static final Pattern EMPTY_PARAGRAPH = Pattern.compile("<p>(\\s|&nbsp;)*</p>");

    private BlogContent() {}

    /** Nội dung HTML cho trang chi tiết: bỏ shortcode WPBakery, giữ chữ, ảnh, link bên trong. */
    public static String cleanHtml(String html) {
        if (html == null) {
            return null;
        }
        String withoutShortcodes = WP_SHORTCODE.matcher(html).replaceAll("");
        return EMPTY_PARAGRAPH.matcher(withoutShortcodes).replaceAll("").trim();
    }

    /**
     * Chữ thuần từ HTML (bỏ thẻ, shortcode, link "Xem bài viết", giải mã {@code &#8230;}...), cắt ở ranh giới từ.
     *
     * @param maxLength độ dài tối đa; dài hơn thì cắt và thêm "…".
     */
    public static String plainText(String html, int maxLength) {
        if (html == null) {
            return null;
        }
        String text = READ_MORE_LINK.matcher(html).replaceAll(" ");
        text = SCRIPT_STYLE.matcher(text).replaceAll(" ");
        text = WP_SHORTCODE.matcher(text).replaceAll(" ");
        text = TAG.matcher(text).replaceAll(" ");
        text = SPACES.matcher(HtmlUtils.htmlUnescape(text)).replaceAll(" ").trim();
        if (text.length() <= maxLength) {
            return text;
        }
        int cut = text.lastIndexOf(' ', maxLength);
        return text.substring(0, cut > maxLength / 2 ? cut : maxLength).trim() + "…";
    }
}
