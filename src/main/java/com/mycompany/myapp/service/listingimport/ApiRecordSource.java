package com.mycompany.myapp.service.listingimport;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Đọc doanh nghiệp từ API JSON của bên thứ ba, lần lượt từng trang (không nạp hết vào bộ nhớ).
 * Mỗi phần tử của mảng dữ liệu là một doanh nghiệp; khóa JSON nhận theo {@link ImportRecord#fieldOf}
 * hoặc theo {@link Request#fieldMapping} (field chuẩn → đường dẫn trong phần tử, vd. {@code "taxCode": "info.mst"}).
 */
public final class ApiRecordSource implements RecordSource {

    @Schema(description = "Cấu hình đọc API bên thứ ba")
    public record Request(
        @Schema(
            description = "Mã nguồn, ghi vào listing.source = api:<mã> (chữ thường, số, gạch ngang)",
            example = "masothue",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        String source,
        @Schema(
            description = "URL GET trả JSON (http/https)",
            example = "https://api.example.com/companies?size=100",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        String url,
        @Schema(description = "Header gửi kèm (khóa API…)", example = "{\"X-API-Key\": \"...\"}") Map<String, String> headers,
        @Schema(description = "Đường dẫn tới mảng doanh nghiệp trong JSON (dấu chấm); trống = JSON gốc là mảng", example = "data.items")
        String itemsPath,
        @Schema(description = "Tên tham số trang thêm vào URL; trống = chỉ gọi 1 lần", example = "page") String pageParam,
        @Schema(description = "Trang đầu (mặc định 1)", example = "1") Integer firstPage,
        @Schema(description = "Số trang tối đa (mặc định 1, tối đa 10000); dừng sớm khi gặp trang rỗng", example = "50") Integer maxPages,
        @Schema(description = "Nghỉ giữa hai trang, ms (mặc định 300) — tránh dồn tải lên API nguồn", example = "300") Integer delayMs,
        @Schema(
            description = "Field chuẩn → đường dẫn trong phần tử (dấu chấm), khi khóa JSON khác tên chuẩn",
            example = "{\"taxCode\": \"mst\", \"name\": \"ten_cong_ty\"}"
        )
        Map<String, String> fieldMapping,
        @Schema(description = "Chạy thử: chỉ phân loại, không ghi vào listing") Boolean dryRun
    ) {}

    private final Request req;
    private final ObjectMapper objectMapper;
    private final HttpClient http;
    private final int firstPage;
    private final int maxPages;
    private final int delayMs;

    private int page;
    private int pagesRead;
    private int rowNo;
    private Iterator<JsonNode> current = List.<JsonNode>of().iterator();
    private boolean exhausted;

    public ApiRecordSource(Request req, ObjectMapper objectMapper) {
        validate(req);
        this.req = req;
        this.objectMapper = objectMapper;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.NORMAL).build();
        this.firstPage = req.firstPage() == null ? 1 : req.firstPage();
        this.maxPages = req.maxPages() == null ? 1 : Math.clamp(req.maxPages(), 1, 10_000);
        this.delayMs = req.delayMs() == null ? 300 : Math.clamp(req.delayMs(), 0, 60_000);
        this.page = firstPage;
    }

    static void validate(Request req) {
        if (req == null || req.source() == null || !req.source().matches("[a-z0-9-]{1,40}")) {
            throw new IllegalArgumentException("source: 1–40 ký tự chữ thường, số, gạch ngang");
        }
        if (req.url() == null || !req.url().matches("(?i)https?://.+")) {
            throw new IllegalArgumentException("url phải bắt đầu bằng http:// hoặc https://");
        }
        if (req.fieldMapping() != null) {
            for (String field : req.fieldMapping().keySet()) {
                if (!List.of(ImportRecord.FIELDS).contains(field)) {
                    throw new IllegalArgumentException(
                        "fieldMapping: không có field \"" + field + "\". Field hợp lệ: " + List.of(ImportRecord.FIELDS)
                    );
                }
            }
        }
    }

    @Override
    public boolean hasNext() {
        while (!current.hasNext() && !exhausted) {
            fetchNextPage();
        }
        return current.hasNext();
    }

    @Override
    public SourceRow next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return new SourceRow(++rowNo, toRecord(current.next()));
    }

    private void fetchNextPage() {
        if (pagesRead >= maxPages) {
            exhausted = true;
            return;
        }
        if (pagesRead > 0 && delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                exhausted = true;
                return;
            }
        }
        String url = req.url();
        if (req.pageParam() != null && !req.pageParam().isBlank()) {
            url += (url.contains("?") ? "&" : "?") + URLEncoder.encode(req.pageParam(), StandardCharsets.UTF_8) + "=" + page;
        }
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(60))
            .header("Accept", "application/json")
            .GET();
        if (req.headers() != null) {
            req.headers().forEach(b::header);
        }
        try {
            HttpResponse<String> res = http.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() / 100 != 2) {
                throw new IllegalStateException("API trả HTTP " + res.statusCode() + " ở trang " + page + ": " + abbreviate(res.body()));
            }
            JsonNode items = path(objectMapper.readTree(res.body()), req.itemsPath());
            if (!items.isArray()) {
                throw new IllegalStateException("itemsPath \"" + req.itemsPath() + "\" không trỏ tới mảng ở trang " + page);
            }
            List<JsonNode> list = new ArrayList<>();
            items.forEach(list::add);
            pagesRead++;
            page++;
            boolean paged = req.pageParam() != null && !req.pageParam().isBlank();
            exhausted = list.isEmpty() || !paged || pagesRead >= maxPages;
            current = list.iterator();
        } catch (IOException e) {
            throw new UncheckedIOException("Không gọi được API " + url, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            exhausted = true;
        }
    }

    private ImportRecord toRecord(JsonNode item) {
        Map<String, String> values = new HashMap<>();
        if (item.isObject()) {
            for (Map.Entry<String, JsonNode> e : item.properties()) {
                String field = ImportRecord.fieldOf(e.getKey());
                if (field != null) {
                    String v = text(e.getValue());
                    if (v != null) {
                        values.putIfAbsent(field, v);
                    }
                }
            }
        }
        if (req.fieldMapping() != null) {
            req.fieldMapping().forEach((field, p) -> {
                String v = text(path(item, p));
                if (v != null) {
                    values.put(field, v);
                }
            });
        }
        return ImportRecord.of(values);
    }

    /** Đường dẫn dấu chấm, phần số là chỉ số mảng: {@code data.items}, {@code contacts.0.phone}. Trống = chính node. */
    static JsonNode path(JsonNode node, String dotted) {
        if (dotted == null || dotted.isBlank()) {
            return node;
        }
        JsonNode n = node;
        for (String part : dotted.split("\\.")) {
            n = part.matches("\\d+") && n.isArray() ? n.path(Integer.parseInt(part)) : n.path(part);
        }
        return n;
    }

    /** Giá trị → chữ: mảng nối bằng "; " (phần tử là object thì lấy name / code / slug), object bỏ qua. */
    static String text(JsonNode n) {
        if (n == null || n.isMissingNode() || n.isNull()) {
            return null;
        }
        if (n.isArray()) {
            List<String> parts = new ArrayList<>();
            for (JsonNode e : n) {
                String t = e.isObject() ? firstText(e, "name", "code", "slug", "value") : text(e);
                if (t != null) {
                    parts.add(t);
                }
            }
            return parts.isEmpty() ? null : String.join("; ", parts);
        }
        if (n.isObject()) {
            return null;
        }
        return ListingImportNormalizer.clean(n.asString());
    }

    private static String firstText(JsonNode o, String... keys) {
        for (String k : keys) {
            String t = text(o.path(k));
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    private static String abbreviate(String s) {
        return s == null ? "" : s.length() > 300 ? s.substring(0, 300) + "…" : s;
    }

    @Override
    public void close() {
        // HttpClient không giữ tài nguyên cần đóng
    }
}
