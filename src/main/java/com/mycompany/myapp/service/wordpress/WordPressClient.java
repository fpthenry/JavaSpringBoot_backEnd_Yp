package com.mycompany.myapp.service.wordpress;

import com.mycompany.myapp.config.ApplicationProperties;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Đọc REST API công khai của WordPress (chỉ GET). Dùng dạng {@code /?rest_route=/wp/v2/...} vì site
 * chuyển hướng đường dẫn {@code /wp-json/}. Chỉ đọc được nội dung đã xuất bản.
 */
@Component
public class WordPressClient {

    private static final Logger LOG = LoggerFactory.getLogger(WordPressClient.class);
    private static final int MAX_ATTEMPTS = 3;

    private final ApplicationProperties.WordPress settings;
    private final ObjectMapper objectMapper;
    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(20))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    public WordPressClient(ApplicationProperties applicationProperties, ObjectMapper objectMapper) {
        this.settings = applicationProperties.getWordpress();
        this.objectMapper = objectMapper;
    }

    /** Một trang kết quả và tổng số trang (header X-WP-TotalPages). */
    public record Page(List<JsonNode> items, int totalPages, int total) {}

    /**
     * Đọc lần lượt mọi trang của {@code route} (vd {@code /wp/v2/posts}), gọi {@code onPage} cho từng trang.
     *
     * @param extraQuery tham số thêm, vd {@code _embed=author,wp:featuredmedia}
     */
    public void forEachPage(String route, String extraQuery, Consumer<Page> onPage) {
        int page = 1;
        int totalPages;
        do {
            Page result = fetchPage(route, extraQuery, page);
            onPage.accept(result);
            totalPages = result.totalPages();
            page++;
        } while (page <= totalPages);
    }

    /** Tổng số bản ghi của {@code route} (đọc 1 bản ghi để lấy header X-WP-Total). */
    public int count(String route) {
        return fetchPage(route, "per_page=1", 1, 1).total();
    }

    private Page fetchPage(String route, String extraQuery, int page) {
        return fetchPage(route, extraQuery, page, settings.getPageSize());
    }

    private Page fetchPage(String route, String extraQuery, int page, int perPage) {
        String url =
            settings.getBaseUrl().replaceAll("/+$", "") +
            "/?rest_route=" +
            URLEncoder.encode(route, StandardCharsets.UTF_8) +
            "&per_page=" +
            perPage +
            "&page=" +
            page +
            "&orderby=id&order=asc" +
            (extraQuery == null || extraQuery.isBlank() ? "" : "&" + extraQuery);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(90))
            .header("User-Agent", "yellowpages-sync/1.0")
            .header("Accept", "application/json")
            .GET()
            .build();
        for (int attempt = 1; ; attempt++) {
            try {
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() != 200) {
                    throw new IOException("HTTP " + response.statusCode() + " " + url);
                }
                JsonNode body = objectMapper.readTree(response.body());
                List<JsonNode> items = new ArrayList<>();
                body.forEach(items::add);
                int totalPages = response.headers().firstValue("X-WP-TotalPages").map(Integer::parseInt).orElse(1);
                int total = response.headers().firstValue("X-WP-Total").map(Integer::parseInt).orElse(items.size());
                return new Page(items, totalPages, total);
            } catch (IOException e) {
                if (attempt >= MAX_ATTEMPTS) {
                    throw new WordPressSyncException("Không đọc được " + url + ": " + e.getMessage(), e);
                }
                LOG.warn("Lỗi đọc {} (lần {}): {}. Thử lại.", url, attempt, e.getMessage());
                sleep(attempt * 2000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new WordPressSyncException("Bị ngắt khi đọc " + url, e);
            }
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Lỗi khi đồng bộ từ WordPress. */
    public static class WordPressSyncException extends RuntimeException {

        public WordPressSyncException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
