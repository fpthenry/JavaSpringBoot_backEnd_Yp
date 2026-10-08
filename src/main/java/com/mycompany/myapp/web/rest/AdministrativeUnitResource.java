package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.service.dvhc.AdministrativeUnitService;
import com.mycompany.myapp.service.dvhc.AdministrativeUnitService.LocationFixReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quản trị đơn vị hành chính sau sáp nhập 01/7/2025. Chỉ ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/admin/administrative-units")
@PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
@Tag(
    name = "administrative-unit-admin",
    description = "Đơn vị hành chính sau sáp nhập 2025: sửa dữ liệu theo danh mục chính thức (ROLE_ADMIN)"
)
public class AdministrativeUnitResource {

    private static final Logger LOG = LoggerFactory.getLogger(AdministrativeUnitResource.class);

    private final AdministrativeUnitService administrativeUnitService;

    public AdministrativeUnitResource(AdministrativeUnitService administrativeUnitService) {
        this.administrativeUnitService = administrativeUnitService;
    }

    @PostMapping("/fix-locations")
    @Operation(
        summary = "Sửa bộ đơn vị mới (2025) trong bảng location theo danh mục chính thức",
        description = "Theo bảng location_conversion (nạp từ file Excel): điền mã cho bộ cũ; với bộ mới sửa mã, tên, slug, " +
            "type = ward cho xã, thêm xã còn thiếu. Không xóa gì; đơn vị không khớp được liệt kê ở unmatched. " +
            "dryRun=true (mặc định): chỉ xem trước. Chạy lại sau mỗi lần đồng bộ location từ jhipster_vnyp."
    )
    public LocationFixReport fixLocations(
        @Parameter(description = "true: chỉ xem trước, không ghi") @RequestParam(name = "dryRun", defaultValue = "true") boolean dryRun
    ) {
        LOG.info("REST request to fix 2025 administrative units, dryRun={}", dryRun);
        return administrativeUnitService.fixLocations(dryRun);
    }
}
