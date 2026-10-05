package com.mycompany.myapp.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import tech.jhipster.config.JHipsterConstants;

/**
 * Bổ sung tài liệu Swagger/OpenAPI cho các API của dự án mà không sửa controller do JHipster sinh ra
 * (sinh lại bằng {@code jhipster jdl --force} sẽ không mất phần này).
 * <p>
 * Nhóm API mặc định của JHipster ({@code springdocDefault}) tự áp dụng mọi bean {@link OpenApiCustomizer}.
 * Xem tài liệu ở {@code /admin/docs} (giao diện quản trị) hoặc {@code /swagger-ui/index.html}.
 */
@Configuration
@Profile(JHipsterConstants.SPRING_PROFILE_API_DOCS)
public class OpenApiDocsConfiguration {

    private static final String JWT_SCHEME = "jwt";

    private static final String API_KEY_SCHEME = "apiKey";

    /** Tiền tố đường dẫn entity -> tên tiếng Việt, dùng để sinh summary CRUD. */
    private static final Map<String, String> ENTITY_NAMES = Map.of(
        "/api/listings",
        "doanh nghiệp",
        "/api/categories",
        "ngành nghề",
        "/api/locations",
        "địa phương",
        "/api/blog-posts",
        "bài viết",
        "/api/tags",
        "thẻ",
        "/api/galleries",
        "vị trí banner",
        "/api/gallery-images",
        "ảnh banner"
    );

    private static final Map<String, String> TAG_DESCRIPTIONS = Map.ofEntries(
        Map.entry("listing-resource", "Doanh nghiệp (1,86 triệu). Lọc theo cây địa phương/ngành nghề bằng locationTreeId, categoryTreeId."),
        Map.entry("category-resource", "Ngành nghề, dạng cây tối đa 4 cấp (nhóm Yellow Pages và hệ thống ngành VSIC)."),
        Map.entry("location-resource", "Địa phương, dạng cây: tỉnh/thành -> quận/huyện -> phường/xã."),
        Map.entry("blog-post-resource", "Bài viết tin tức."),
        Map.entry("tag-resource", "Thẻ bài viết."),
        Map.entry("authenticate-controller", "Đăng nhập, lấy JWT. Gửi JWT ở header Authorization: Bearer <token>."),
        Map.entry("account-resource", "Tài khoản của người dùng đang đăng nhập: đăng ký, kích hoạt, đổi/đặt lại mật khẩu."),
        Map.entry("user-resource", "Quản lý người dùng (ROLE_ADMIN)."),
        Map.entry("public-user-resource", "Danh sách người dùng công khai (chỉ id và login)."),
        Map.entry("authority-resource", "Quyền (ROLE_ADMIN, ROLE_USER)."),
        Map.entry("gallery-resource", "Quản trị vị trí banner quảng cáo (gallery)."),
        Map.entry("gallery-image-resource", "Quản trị ảnh banner: upload ảnh hoặc dán link ảnh, link đích, thứ tự, hẹn giờ.")
    );

    /** Summary cho các API không theo mẫu CRUD entity. Khóa: "METHOD path". */
    private static final Map<String, String> OTHER_SUMMARIES = Map.ofEntries(
        Map.entry("POST /api/authenticate", "Đăng nhập, trả về JWT (id_token)"),
        Map.entry("GET /api/authenticate", "Kiểm tra JWT hiện tại còn hợp lệ"),
        Map.entry("POST /api/register", "Đăng ký tài khoản mới"),
        Map.entry("GET /api/activate", "Kích hoạt tài khoản bằng key gửi qua email"),
        Map.entry("GET /api/account", "Thông tin tài khoản đang đăng nhập"),
        Map.entry("POST /api/account", "Cập nhật thông tin tài khoản đang đăng nhập"),
        Map.entry("POST /api/account/change-password", "Đổi mật khẩu"),
        Map.entry("POST /api/account/reset-password/init", "Yêu cầu đặt lại mật khẩu (gửi email)"),
        Map.entry("POST /api/account/reset-password/finish", "Đặt lại mật khẩu bằng key trong email"),
        Map.entry("GET /api/users", "Danh sách người dùng công khai"),
        Map.entry("GET /api/users/_search/{query}", "Tìm người dùng (Elasticsearch)"),
        Map.entry("GET /api/blog-posts/_search", "Tìm bài viết (Elasticsearch, không dấu), lọc theo danh mục, thẻ, trạng thái"),
        Map.entry("GET /api/admin/users", "Danh sách người dùng (quản trị)"),
        Map.entry("POST /api/admin/users", "Tạo người dùng"),
        Map.entry("PUT /api/admin/users", "Cập nhật người dùng"),
        Map.entry("PUT /api/admin/users/{login}", "Cập nhật người dùng theo login"),
        Map.entry("GET /api/admin/users/{login}", "Chi tiết người dùng"),
        Map.entry("DELETE /api/admin/users/{login}", "Xóa người dùng"),
        Map.entry("GET /api/authorities", "Danh sách quyền"),
        Map.entry("POST /api/authorities", "Tạo quyền"),
        Map.entry("GET /api/authorities/{id}", "Chi tiết quyền"),
        Map.entry("DELETE /api/authorities/{id}", "Xóa quyền")
    );

    /** Filter theo cây chỉ xử lý toán tử equals (xem ListingQueryService#createSpecification). */
    private static final Map<String, String> TREE_FILTERS = Map.of(
        "locationTreeId",
        "Lọc doanh nghiệp thuộc địa phương này hoặc bất kỳ cấp con nào (tỉnh -> quận/huyện -> phường/xã). " +
            "Khác locationId.equals (chỉ khớp đúng địa phương được gắn). Ví dụ Hà Nội (id 2): 265.942 doanh nghiệp.",
        "categoryTreeId",
        "Lọc doanh nghiệp thuộc ngành này hoặc bất kỳ ngành con nào (tối đa 4 cấp). " +
            "Khác categoryId.equals (chỉ khớp đúng ngành được gắn). Nhóm ngành VSIC rất lớn có thể mất vài chục giây."
    );

    private static final String TREE_PARENT_DESCRIPTION =
        "Dùng để duyệt cây: parentId.specified=false lấy các nút gốc, parentId.equals={id} lấy các nút con.";

    @Bean
    public OpenApiCustomizer yellowPagesOpenApiCustomizer() {
        return openApi -> {
            addJwtSecurity(openApi);
            addTagDescriptions(openApi);
            if (openApi.getPaths() != null) {
                openApi.getPaths().forEach(this::describePath);
            }
        };
    }

    private void addJwtSecurity(OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
        openApi
            .getComponents()
            .addSecuritySchemes(
                JWT_SCHEME,
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Lấy token bằng POST /api/authenticate, rồi bấm Authorize và dán id_token.")
            );
        openApi.addSecurityItem(new SecurityRequirement().addList(JWT_SCHEME));
        openApi
            .getComponents()
            .addSecuritySchemes(
                API_KEY_SCHEME,
                new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name("X-API-Key")
                    .description("Khóa API công khai cho FE (Next.js), cấu hình application.public-api.keys.")
            );
    }

    private void addTagDescriptions(OpenAPI openApi) {
        TAG_DESCRIPTIONS.forEach((name, description) -> {
            Tag tag =
                openApi.getTags() == null
                    ? null
                    : openApi
                          .getTags()
                          .stream()
                          .filter(t -> name.equals(t.getName()))
                          .findFirst()
                          .orElse(null);
            if (tag == null) {
                openApi.addTagsItem(new Tag().name(name).description(description));
            } else if (tag.getDescription() == null) {
                tag.setDescription(description);
            }
        });
    }

    private void describePath(String path, PathItem item) {
        item.readOperationsMap().forEach((method, operation) -> {
            String summary = summaryFor(method.name(), path);
            if (summary != null && operation.getSummary() == null) {
                operation.setSummary(summary);
            }
            describeParameters(operation);
            if (path.startsWith("/api/public/")) {
                operation.setSecurity(
                    path.startsWith("/api/public/v1/media/") ? List.of() : List.of(new SecurityRequirement().addList(API_KEY_SCHEME))
                );
            }
        });
    }

    private String summaryFor(String method, String path) {
        String other = OTHER_SUMMARIES.get(method + " " + path);
        if (other != null) {
            return other;
        }
        for (Map.Entry<String, String> entity : ENTITY_NAMES.entrySet()) {
            String base = entity.getKey();
            String name = entity.getValue();
            if (path.equals(base)) {
                return switch (method) {
                    case "GET" -> "Danh sách " + name + " (phân trang, sắp xếp, lọc theo các field)";
                    case "POST" -> "Tạo " + name;
                    default -> null;
                };
            }
            if (path.equals(base + "/{id}")) {
                return switch (method) {
                    case "GET" -> "Chi tiết " + name;
                    case "PUT" -> "Cập nhật toàn bộ " + name;
                    case "PATCH" -> "Cập nhật một phần " + name + " (chỉ các field gửi lên)";
                    case "DELETE" -> "Xóa " + name;
                    default -> null;
                };
            }
            if (path.equals(base + "/count")) {
                return "Đếm " + name + " theo cùng bộ lọc với API danh sách";
            }
            if (path.equals(base + "/_search")) {
                return "Tìm kiếm " + name + " (Elasticsearch, không áp dụng bộ lọc)";
            }
        }
        return null;
    }

    private void describeParameters(Operation operation) {
        List<Parameter> parameters = operation.getParameters();
        if (parameters == null) {
            return;
        }
        // Filter theo cây chỉ hỗ trợ .equals: bỏ các toán tử khác để người dùng không nhầm
        parameters.removeIf(p -> isTreeFilter(p.getName()) && !p.getName().endsWith(".equals"));
        for (Parameter parameter : parameters) {
            String name = parameter.getName();
            if (isTreeFilter(name)) {
                parameter.setDescription(TREE_FILTERS.get(name.substring(0, name.indexOf('.'))));
            } else if (name.equals("parentId.specified") || name.equals("parentId.equals")) {
                parameter.setDescription(TREE_PARENT_DESCRIPTION);
            }
        }
    }

    private static boolean isTreeFilter(String parameterName) {
        int dot = parameterName.indexOf('.');
        return dot > 0 && TREE_FILTERS.containsKey(parameterName.substring(0, dot));
    }
}
