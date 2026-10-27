package com.mycompany.myapp.service.listingimport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.CellType;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;

/**
 * Đọc file nhập: Excel .xlsx (sheet đầu) hoặc CSV (UTF-8, có/không BOM; phân cách , ; hoặc tab tự nhận).
 * Dòng đầu là tiêu đề cột; tên cột nhận theo {@link ImportRecord#fieldOf} (tiếng Việt có/không dấu, tiếng Anh).
 */
public final class ImportFileReader {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ImportFileReader() {}

    /** @throws IllegalArgumentException file không phải .xlsx/.csv, hoặc không có cột tên doanh nghiệp */
    public static RecordSource open(Path file, String fileName) throws IOException {
        String lower = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".xlsx")) {
            return new ExcelSource(file);
        }
        if (lower.endsWith(".csv") || lower.endsWith(".txt")) {
            return new CsvSource(file);
        }
        if (lower.endsWith(".xls")) {
            throw new IllegalArgumentException("File .xls (Excel 97-2003) chưa hỗ trợ: mở bằng Excel rồi lưu lại dạng .xlsx hoặc .csv");
        }
        throw new IllegalArgumentException("Chỉ nhận file .xlsx hoặc .csv");
    }

    /** Cột thứ i → field chuẩn (null: cột bỏ qua). Báo lỗi nếu không có cột tên. */
    static String[] mapHeaders(List<String> headers) {
        String[] fields = new String[headers.size()];
        boolean hasName = false;
        for (int i = 0; i < headers.size(); i++) {
            fields[i] = ImportRecord.fieldOf(headers.get(i));
            hasName |= "name".equals(fields[i]);
        }
        if (!hasName) {
            throw new IllegalArgumentException(
                "Không thấy cột tên doanh nghiệp (\"Tên doanh nghiệp\", \"Tên công ty\" hoặc \"name\"). Tiêu đề đọc được: " + headers
            );
        }
        return fields;
    }

    // ---------------------------------------------------------------- CSV

    static final class CsvSource implements RecordSource {

        private final Reader reader;
        private final CSVParser parser;
        private final Iterator<CSVRecord> records;
        private final String[] fields;

        CsvSource(Path file) throws IOException {
            char delimiter = detectDelimiter(file);
            this.reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
            reader.mark(1);
            if (reader.read() != '﻿') {
                reader.reset(); // không có BOM
            }
            // Tự đọc dòng tiêu đề (không dùng setHeader): cột tiêu đề trống / trùng tên vẫn giữ đúng vị trí cột
            this.parser = CSVFormat.DEFAULT.builder().setDelimiter(delimiter).setIgnoreEmptyLines(true).setTrim(true).build().parse(reader);
            this.records = parser.iterator();
            if (!records.hasNext()) {
                throw new IllegalArgumentException("File rỗng");
            }
            this.fields = mapHeaders(records.next().toList());
        }

        private static char detectDelimiter(Path file) throws IOException {
            try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String first = r.readLine();
                if (first == null) {
                    throw new IllegalArgumentException("File rỗng");
                }
                long comma = first
                    .chars()
                    .filter(c -> c == ',')
                    .count();
                long semicolon = first
                    .chars()
                    .filter(c -> c == ';')
                    .count();
                long tab = first
                    .chars()
                    .filter(c -> c == '\t')
                    .count();
                if (tab > comma && tab > semicolon) {
                    return '\t';
                }
                return semicolon > comma ? ';' : ',';
            }
        }

        @Override
        public boolean hasNext() {
            return records.hasNext();
        }

        @Override
        public SourceRow next() {
            CSVRecord r = records.next();
            Map<String, String> values = new HashMap<>();
            for (int i = 0; i < fields.length && i < r.size(); i++) {
                put(values, fields[i], r.get(i));
            }
            return new SourceRow((int) r.getRecordNumber(), ImportRecord.of(values)); // bản ghi 1 là tiêu đề
        }

        @Override
        public void close() {
            try {
                parser.close();
                reader.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    // ---------------------------------------------------------------- Excel

    static final class ExcelSource implements RecordSource {

        private final InputStream in;
        private final ReadableWorkbook workbook;
        private final Stream<Row> stream;
        private final Iterator<Row> rows;
        private final String[] fields;
        private Row nextRow;

        ExcelSource(Path file) throws IOException {
            this.in = Files.newInputStream(file);
            this.workbook = new ReadableWorkbook(in);
            this.stream = workbook.getFirstSheet().openStream();
            this.rows = stream.iterator();
            Row header = null;
            while (rows.hasNext() && header == null) {
                Row r = rows.next();
                if (r.getFirstNonEmptyCell().isPresent()) {
                    header = r;
                }
            }
            if (header == null) {
                throw new IllegalArgumentException("Sheet đầu tiên không có dữ liệu");
            }
            List<String> headers = new ArrayList<>();
            for (int i = 0; i < header.getCellCount(); i++) {
                String h = text(header, i, null);
                headers.add(h == null ? "" : h);
            }
            this.fields = mapHeaders(headers);
            advance();
        }

        /** Bỏ qua dòng trống. */
        private void advance() {
            nextRow = null;
            while (rows.hasNext()) {
                Row r = rows.next();
                if (r.getFirstNonEmptyCell().isPresent()) {
                    nextRow = r;
                    return;
                }
            }
        }

        @Override
        public boolean hasNext() {
            return nextRow != null;
        }

        @Override
        public SourceRow next() {
            if (nextRow == null) {
                throw new NoSuchElementException();
            }
            Row r = nextRow;
            Map<String, String> values = new HashMap<>();
            for (int i = 0; i < fields.length; i++) {
                if (fields[i] != null) {
                    put(values, fields[i], text(r, i, fields[i]));
                }
            }
            advance();
            return new SourceRow(r.getRowNum(), ImportRecord.of(values));
        }

        /** Ô số: viết đủ chữ số (MST 101234567, không phải 1.01234567E8); ô ngày (ngày thành lập) → dd/MM/yyyy. */
        private String text(Row row, int i, String field) {
            if (i >= row.getCellCount()) {
                return null;
            }
            Cell c = row.getCell(i);
            if (c == null || c.getType() == CellType.EMPTY) {
                return null;
            }
            if (c.getType() == CellType.NUMBER) {
                BigDecimal n = c.asNumber();
                String fmt = c.getDataFormatString();
                boolean dateFormat = fmt != null && fmt.matches("(?i).*[dmy].*") && !fmt.contains("0");
                if ("foundedYear".equals(field) && (dateFormat || (n.scale() <= 0 && n.intValue() > 3000 && n.intValue() < 80000))) {
                    return LocalDate.of(1899, 12, 30).plusDays(n.longValue()).format(DATE);
                }
                return n.stripTrailingZeros().toPlainString();
            }
            return c.getText();
        }

        @Override
        public void close() {
            try {
                stream.close();
                workbook.close();
                in.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static void put(Map<String, String> values, String field, String value) {
        if (field == null) {
            return;
        }
        String v = ListingImportNormalizer.clean(value);
        if (v != null) {
            values.merge(field, v, (a, b) -> a + "; " + b); // hai cột cùng nghĩa (vd. SĐT 1, SĐT 2) → nối lại
        }
    }
}
