package com.mycompany.myapp.repository.search;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.data.elasticsearch.core.mapping.PropertyValueConverter;

/**
 * Chuẩn hóa chữ tiếng Việt trước khi đưa vào Elasticsearch và trước khi tìm, để tìm đúng dấu không bị sót bài:
 * <ul>
 *   <li><b>Unicode dựng sẵn (NFC)</b>: chữ gõ kiểu "Unicode tổ hợp" (dấu tách rời, ví dụ {@code ô + U+0323}) thành một ký tự {@code ộ}.</li>
 *   <li><b>Kiểu đặt dấu mới</b> cho vần mở oa, oe, uy: {@code hoà → hòa}, {@code thuỷ → thủy}, {@code uỷ → ủy} (trừ {@code quý}).
 *   Vần có phụ âm cuối như {@code hoàn}, {@code xoáy} giống nhau ở cả hai kiểu nên giữ nguyên.</li>
 * </ul>
 * Chỉ dùng cho ES: dữ liệu trong MySQL không bị sửa.
 */
public final class VietnameseText {

    /** o + a/e mang dấu, hoặc u + y mang dấu, ở cuối âm tiết (sau đó không còn chữ cái). */
    private static final Pattern OLD_TONE_PLACEMENT = Pattern.compile("(?<![qQ])([oOuU])([àáảãạèéẻẽẹỳýỷỹỵÀÁẢÃẠÈÉẺẼẸỲÝỶỸỴ])(?!\\p{L})");
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_WORD = Pattern.compile("[^\\p{L}\\p{N}]+");

    /** Số từ tối đa của một truy vấn, tránh truy vấn quá lớn. */
    static final int MAX_WORDS = 20;

    private VietnameseText() {}

    public static String normalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String nfc = Normalizer.normalize(text, Normalizer.Form.NFC);
        Matcher matcher = OLD_TONE_PLACEMENT.matcher(nfc);
        StringBuilder result = new StringBuilder(nfc.length());
        while (matcher.find()) {
            matcher.appendReplacement(result, Matcher.quoteReplacement(moveTone(matcher.group(1), matcher.group(2))));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /** {@code o + à → ò + a}; cặp không phải oa, oe, uy thì giữ nguyên. */
    private static String moveTone(String first, String toned) {
        String decomposed = Normalizer.normalize(toned, Normalizer.Form.NFD);
        String base = decomposed.substring(0, 1);
        boolean pair = first.equalsIgnoreCase("o") ? "aeAE".contains(base) : "yY".contains(base);
        if (!pair) {
            return first + toned;
        }
        return Normalizer.normalize(first + decomposed.substring(1), Normalizer.Form.NFC) + base;
    }

    /** Bỏ dấu: {@code Hà Nội → Ha Noi}, {@code đ → d}. */
    public static String fold(String text) {
        String withoutMarks = MARKS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
        return withoutMarks.replace('đ', 'd').replace('Đ', 'D');
    }

    public static boolean hasDiacritics(String word) {
        return !fold(word).equals(word);
    }

    /** Tách truy vấn (đã chuẩn hóa) thành các từ, bỏ dấu câu, tối đa {@link #MAX_WORDS} từ. */
    public static List<String> words(String text) {
        if (text == null) {
            return List.of();
        }
        return Arrays.stream(NON_WORD.split(text))
            .filter(word -> !word.isEmpty())
            .limit(MAX_WORDS)
            .toList();
    }

    /** Gắn vào field của entity ({@code @ValueConverter}) để chuẩn hóa khi ghi vào ES; đọc ra giữ nguyên. */
    public static class EsConverter implements PropertyValueConverter {

        @Override
        public Object write(Object value) {
            return value instanceof String text ? normalize(text) : value;
        }

        @Override
        public Object read(Object value) {
            return value;
        }
    }
}
