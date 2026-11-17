package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.SiteSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Viết tay: cấu hình chung của website (khóa/giá trị). */
@Repository
public interface SiteSettingRepository extends JpaRepository<SiteSetting, String> {}
