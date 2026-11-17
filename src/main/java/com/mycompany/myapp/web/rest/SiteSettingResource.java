package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.service.SiteSettingService;
import com.mycompany.myapp.service.SiteSettingService.Effects;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Viết tay (2026-11-17): hiệu ứng trang trí website (hiện có: hiệu ứng Tết hoa đào).
 * Quyền ở SecurityConfiguration: xem cho mọi nhân viên, bật/tắt cho ROLE_ADMIN và ROLE_BANNER_EDIT.
 */
@RestController
@RequestMapping("/api/site-settings")
@Tag(name = "site-settings", description = "Hiệu ứng trang trí website (Tết hoa đào)")
public class SiteSettingResource {

    private final SiteSettingService siteSettingService;

    public SiteSettingResource(SiteSettingService siteSettingService) {
        this.siteSettingService = siteSettingService;
    }

    /** Thân request bật/tắt. */
    public record Toggle(@NotNull Boolean enabled) {}

    @GetMapping("/effects")
    @Operation(summary = "Trạng thái các hiệu ứng website, kèm người sửa gần nhất")
    public Effects getEffects() {
        return siteSettingService.getEffects();
    }

    @PutMapping("/effects/tet")
    @Operation(summary = "Bật/tắt hiệu ứng Tết hoa đào trên yp.vn", description = "Website thấy thay đổi sau tối đa khoảng 1 phút.")
    public Effects setTetEffect(@Valid @RequestBody Toggle toggle) {
        return siteSettingService.setTetEffect(toggle.enabled());
    }
}
