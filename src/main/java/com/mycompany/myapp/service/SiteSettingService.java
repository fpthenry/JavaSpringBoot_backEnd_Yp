package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.SiteSetting;
import com.mycompany.myapp.repository.SiteSettingRepository;
import com.mycompany.myapp.security.SecurityUtils;
import java.io.Serializable;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Viết tay (2026-11-17): đọc/ghi cấu hình chung của website. Hiện có một khóa: {@link #TET_EFFECT} bật hiệu ứng Tết
 * (hoa đào rơi) trên toàn bộ yp.vn. Chưa có dòng trong bảng = tắt.
 */
@Service
@Transactional
public class SiteSettingService {

    public static final String TET_EFFECT = "tet_effect";

    private static final Logger LOG = LoggerFactory.getLogger(SiteSettingService.class);

    private final SiteSettingRepository repository;

    public SiteSettingService(SiteSettingRepository repository) {
        this.repository = repository;
    }

    /** Cấu hình hiển thị trong trang quản trị: giá trị kèm ai sửa, lúc nào. */
    public record Effects(boolean tetEffect, Instant updatedAt, String updatedBy) implements Serializable {}

    /** Cấu hình FE (Next.js) cần, qua API công khai: chỉ giá trị, không lộ ai sửa. */
    public record PublicSettings(boolean tetEffect) implements Serializable {}

    @Transactional(readOnly = true)
    public Effects getEffects() {
        return repository
            .findById(TET_EFFECT)
            .map(setting -> new Effects(Boolean.parseBoolean(setting.getValue()), setting.getUpdatedAt(), setting.getUpdatedBy()))
            .orElseGet(() -> new Effects(false, null, null));
    }

    @Transactional(readOnly = true)
    public PublicSettings getPublicSettings() {
        return new PublicSettings(getEffects().tetEffect());
    }

    public Effects setTetEffect(boolean enabled) {
        SiteSetting setting = repository.findById(TET_EFFECT).orElseGet(() -> {
            SiteSetting created = new SiteSetting();
            created.setKey(TET_EFFECT);
            return created;
        });
        setting.setValue(Boolean.toString(enabled));
        setting.setUpdatedAt(Instant.now());
        setting.setUpdatedBy(SecurityUtils.getCurrentUserLogin().orElse("system"));
        repository.saveAndFlush(setting);
        LOG.info("Hiệu ứng Tết: {} (bởi {})", enabled ? "BẬT" : "TẮT", setting.getUpdatedBy());
        return new Effects(enabled, setting.getUpdatedAt(), setting.getUpdatedBy());
    }
}
