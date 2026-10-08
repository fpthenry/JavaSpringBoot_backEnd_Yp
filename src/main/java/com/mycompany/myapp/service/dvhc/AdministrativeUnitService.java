package com.mycompany.myapp.service.dvhc;

import com.mycompany.myapp.service.ElasticsearchReindexService;
import com.mycompany.myapp.service.dvhc.LocationFixPlanner.LocationRow;
import com.mycompany.myapp.service.dvhc.LocationFixPlanner.OfficialUnit;
import com.mycompany.myapp.service.dvhc.LocationFixPlanner.Plan;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Đơn vị hành chính sau sáp nhập 01/7/2025: sửa bộ đơn vị mới trong bảng {@code location} theo danh mục chính thức
 * ({@code location_conversion}, nạp từ file Excel). Xem {@link LocationFixPlanner}.
 * <p>
 * Chạy lại được (lần sau không còn gì để sửa). Cần chạy lại sau mỗi lần đồng bộ {@code location} từ {@code jhipster_vnyp}.
 */
@Service
public class AdministrativeUnitService {

    private static final Logger LOG = LoggerFactory.getLogger(AdministrativeUnitService.class);

    private final JdbcTemplate jdbcTemplate;
    /** Hikari đặt auto-commit: false, nên mọi lệnh ghi phải chạy trong transaction. */
    private final TransactionTemplate transaction;
    private final ElasticsearchReindexService reindexService;

    public AdministrativeUnitService(
        DataSource dataSource,
        PlatformTransactionManager transactionManager,
        ElasticsearchReindexService reindexService
    ) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.transaction = new TransactionTemplate(transactionManager);
        this.reindexService = reindexService;
    }

    /**
     * Kết quả sửa bảng location.
     *
     * @param dryRun    true: chỉ lập kế hoạch, chưa ghi gì.
     * @param stats     số thay đổi theo loại.
     * @param unmatched đơn vị không khớp được (không bị xóa, cần xem tay).
     * @param renames   tất cả xã đổi tên (khớp theo mã) và xã thêm mới.
     * @param samples   ví dụ thay đổi (đổi tên, sửa mã, thêm mới).
     */
    public record LocationFixReport(
        boolean dryRun,
        int updatedRows,
        int insertedRows,
        Map<String, Integer> stats,
        List<String> unmatched,
        List<String> renames,
        List<String> samples,
        Instant finishedAt
    ) {}

    public LocationFixReport fixLocations(boolean dryRun) {
        Plan plan = LocationFixPlanner.plan(loadLocations(), loadOfficialProvinces(), loadOfficialWards());
        LOG.info(
            "Sửa location theo danh mục 2025 (dryRun={}): {} cập nhật, {} thêm, {} không khớp. {}",
            dryRun,
            plan.updates().size(),
            plan.inserts().size(),
            plan.unmatched().size(),
            plan.stats()
        );
        if (!dryRun && !plan.isEmpty()) {
            apply(plan);
            try {
                reindexService.start(List.of("location"));
            } catch (IllegalStateException e) {
                LOG.warn("Không reindex được location vì đang có job reindex khác; chạy lại: reindex?entities=location");
            }
        }
        return new LocationFixReport(
            dryRun,
            plan.updates().size(),
            plan.inserts().size(),
            plan.stats(),
            plan.unmatched(),
            plan.renames(),
            plan.samples(),
            Instant.now()
        );
    }

    private void apply(Plan plan) {
        Timestamp now = Timestamp.from(Instant.now());
        transaction.executeWithoutResult(tx -> {
            jdbcTemplate.batchUpdate(
                "update location set name = ?, slug = ?, type = ?, code = ?, parent_id = ? where id = ?",
                plan.updates(),
                500,
                (ps, u) -> {
                    ps.setString(1, u.name());
                    ps.setString(2, u.slug());
                    ps.setString(3, u.type());
                    ps.setString(4, u.code());
                    ps.setObject(5, u.parentId());
                    ps.setLong(6, u.id());
                }
            );
            jdbcTemplate.batchUpdate(
                "insert into location (name, slug, type, code, parent_id, created_at) values (?, ?, ?, ?, ?, ?)",
                plan.inserts(),
                500,
                (ps, i) -> {
                    ps.setString(1, i.name());
                    ps.setString(2, i.slug());
                    ps.setString(3, i.type());
                    ps.setString(4, i.code());
                    ps.setLong(5, i.parentId());
                    ps.setTimestamp(6, now);
                }
            );
        });
    }

    List<LocationRow> loadLocations() {
        return jdbcTemplate.query("select id, name, slug, type, code, parent_id from location", (rs, n) -> {
            // getObject: wasNull() chỉ đúng với cột đọc ngay trước nó, dễ nhầm khi đọc nhiều cột
            Long parent = rs.getObject("parent_id", Long.class);
            return new LocationRow(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("slug"),
                rs.getString("type"),
                rs.getString("code"),
                parent
            );
        });
    }

    List<OfficialUnit> loadOfficialProvinces() {
        return jdbcTemplate.query(
            "select new_province_code, min(new_province_name) from location_conversion group by new_province_code",
            (rs, n) -> new OfficialUnit(null, rs.getString(1), rs.getString(2))
        );
    }

    List<OfficialUnit> loadOfficialWards() {
        return jdbcTemplate.query(
            "select new_province_code, new_ward_code, min(new_ward_name) from location_conversion group by new_province_code, new_ward_code",
            (rs, n) -> new OfficialUnit(rs.getString(1), rs.getString(2), rs.getString(3))
        );
    }
}
