package com.mycompany.myapp.web.rest.publicapi;

import com.mycompany.myapp.service.SiteSettingService;
import com.mycompany.myapp.service.SiteSettingService.PublicSettings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Viết tay (2026-11-17): cấu hình chung cho FE (Next.js), ví dụ có bật hiệu ứng Tết không. Cần header X-API-Key. */
@RestController
@RequestMapping("/api/public/v1")
@Tag(name = "public-site-settings", description = "API công khai: cấu hình chung của website (cần header X-API-Key)")
public class PublicSiteSettingResource {

    /** Quản trị viên bật/tắt thì website thấy sau tối đa chừng ấy. */
    private static final CacheControl CACHE = CacheControl.maxAge(Duration.ofMinutes(1)).cachePublic();

    private final SiteSettingService siteSettingService;

    public PublicSiteSettingResource(SiteSettingService siteSettingService) {
        this.siteSettingService = siteSettingService;
    }

    @GetMapping("/site-settings")
    @Operation(summary = "Cấu hình chung của website", description = "tetEffect = true: hiện hiệu ứng Tết (hoa đào rơi) trên toàn site.")
    public ResponseEntity<PublicSettings> getSiteSettings() {
        return ResponseEntity.ok().cacheControl(CACHE).body(siteSettingService.getPublicSettings());
    }
}
