package com.mycompany.myapp.web.rest.publicapi;

import com.mycompany.myapp.service.SiteSettingService;
import com.mycompany.myapp.service.SiteSettingService.PublicSettings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Viết tay (2026-11-17): cấu hình chung cho FE (Next.js), ví dụ có bật hiệu ứng Tết không. Cần header X-API-Key. Không cache. */
@RestController
@RequestMapping("/api/public/v1")
@Tag(name = "public-site-settings", description = "API công khai: cấu hình chung của website (cần header X-API-Key)")
public class PublicSiteSettingResource {

    /**
     * Không cache: quản trị viên bật/tắt là website thấy ngay ở lần tải trang kế tiếp (trước đây cache 1 phút làm
     * tưởng bấm không ăn). Dữ liệu chỉ một dòng nên đọc mỗi lần không đáng kể.
     */
    private static final CacheControl CACHE = CacheControl.noStore();

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
