package com.mycompany.myapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties specific to Java Spring Boot Back End.
 * <p>
 * Properties are configured in the {@code application.yml} file.
 * See {@link tech.jhipster.config.JHipsterProperties} for a good example.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

    private final Liquibase liquibase = new Liquibase();

    private final PublicApi publicApi = new PublicApi();

    // jhipster-needle-application-properties-property

    public Liquibase getLiquibase() {
        return liquibase;
    }

    public PublicApi getPublicApi() {
        return publicApi;
    }

    // jhipster-needle-application-properties-property-getter

    /**
     * API công khai {@code /api/public/**} cho FE (Next.js): xác thực bằng header {@code X-API-Key}.
     * Cấu hình {@code application.public-api.keys}; production đặt bằng biến môi trường
     * {@code APPLICATION_PUBLIC_API_KEYS} (nhiều khóa cách nhau bởi dấu phẩy, để đổi khóa không gián đoạn).
     * Danh sách rỗng thì mọi request tới API công khai đều bị từ chối.
     */
    public static class PublicApi {

        private java.util.List<String> keys = new java.util.ArrayList<>();

        public java.util.List<String> getKeys() {
            return keys;
        }

        public void setKeys(java.util.List<String> keys) {
            this.keys = keys;
        }
    }

    public static class Liquibase {

        private Boolean asyncStart = true;

        public Boolean getAsyncStart() {
            return asyncStart;
        }

        public void setAsyncStart(Boolean asyncStart) {
            this.asyncStart = asyncStart;
        }
    }

    // jhipster-needle-application-properties-property-class
}
