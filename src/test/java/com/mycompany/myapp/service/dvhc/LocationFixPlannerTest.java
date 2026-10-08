package com.mycompany.myapp.service.dvhc;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.service.dvhc.LocationFixPlanner.Insert;
import com.mycompany.myapp.service.dvhc.LocationFixPlanner.LocationRow;
import com.mycompany.myapp.service.dvhc.LocationFixPlanner.OfficialUnit;
import com.mycompany.myapp.service.dvhc.LocationFixPlanner.Plan;
import com.mycompany.myapp.service.dvhc.LocationFixPlanner.Update;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class LocationFixPlannerTest {

    private static final List<OfficialUnit> PROVINCES = List.of(new OfficialUnit(null, "01", "Thành phố Hà Nội"));

    private static final List<OfficialUnit> WARDS = List.of(
        new OfficialUnit("01", "00082", "Phường Cửa Nam"),
        new OfficialUnit("01", "00226", "Phường Văn Miếu - Quốc Tử Giám"),
        new OfficialUnit("01", "00229", "Phường Kim Liên"),
        new OfficialUnit("01", "00004", "Phường Ba Đình"),
        new OfficialUnit("01", "01501", "Xã Đàm Thủy"),
        new OfficialUnit("01", "20152", "Xã Chân Mây - Lăng Cô"),
        new OfficialUnit("01", "01537", "Xã Lý Quốc"),
        new OfficialUnit("01", "00070", "Phường Hoàn Kiếm")
    );

    private static final List<LocationRow> ROWS = List.of(
        // Bộ cũ: chưa có mã
        new LocationRow(2, "Thành phố Hà Nội", "thanh-pho-ha-noi-01", "province", null, null),
        new LocationRow(256, "Huyện Ba Vì", "huyen-ba-vi-271", "district", null, 2L),
        new LocationRow(4090, "Thị trấn Tây Đằng", "thi-tran-tay-dang-09619", "ward", "09619", 256L),
        // Bộ mới
        new LocationRow(11960, "Thành phố Hà Nội", "thanh-pho-ha-noi-01-2025", "province", null, null),
        new LocationRow(11995, "Phường Cửa Nam", "phuong-cua-nam-00073-2025", "district", null, 11960L), // sai mã
        new LocationRow(12010, "Phường Kim Liên", "phuong-kim-lien-00226-2025", "district", null, 11960L), // mang mã của Văn Miếu
        new LocationRow(12011, "Phường Văn Miếu - Quốc Tử Giám", "phuong-van-mieu-quoc-tu-giam-00196-2025", "district", null, 11960L),
        new LocationRow(11996, "Phường Ba Đình", "phuong-ba-dinh-00004-2025", "ward", "00004", 11960L), // đã đúng
        new LocationRow(12020, "Xã Đàm Thuỷ", "xa-dam-thuy-01501-2025", "district", null, 11960L), // kiểu đặt dấu cũ
        new LocationRow(12021, "Xã Chân Mây – Lăng Cô", "xa-chan-may-lang-co-20152-2025", "district", null, 11960L), // gạch dài
        new LocationRow(12022, "Xã Vinh Quý", "xa-vinh-quy-01537-2025", "district", null, 11960L), // đổi tên, cùng mã
        new LocationRow(12023, "Xã Không Có", "xa-khong-co-99999-2025", "district", null, 11960L) // không có trong danh mục
    );

    private static Map<Long, Update> byId(Plan plan) {
        return plan.updates().stream().collect(Collectors.toMap(Update::id, Function.identity()));
    }

    @Test
    void boCuDienMaTuSlug() {
        Map<Long, Update> updates = byId(LocationFixPlanner.plan(ROWS, PROVINCES, WARDS));
        assertThat(updates.get(2L).code()).isEqualTo("01");
        assertThat(updates.get(256L).code()).isEqualTo("271");
        assertThat(updates.get(256L).type()).isEqualTo("district");
        assertThat(updates).doesNotContainKey(4090L); // mã đã đúng
    }

    @Test
    void xaMoiKhopTenTruocSuaMaSaiVaMaBiGanLan() {
        Plan plan = LocationFixPlanner.plan(ROWS, PROVINCES, WARDS);
        Map<Long, Update> updates = byId(plan);
        assertThat(updates.get(11995L)).isEqualTo(
            new Update(11995, "Phường Cửa Nam", "phuong-cua-nam-00082-2025", "ward", "00082", 11960L)
        );
        assertThat(updates.get(12010L).code()).isEqualTo("00229");
        assertThat(updates.get(12011L).code()).isEqualTo("00226");
        assertThat(updates.get(12011L).slug()).isEqualTo("phuong-van-mieu-quoc-tu-giam-00226-2025");
        assertThat(updates).doesNotContainKey(11996L);
        assertThat(updates.get(11960L).code()).isEqualTo("01");
    }

    @Test
    void kieuDatDauGachDaiVaDoiTenCungMa() {
        Plan plan = LocationFixPlanner.plan(ROWS, PROVINCES, WARDS);
        Map<Long, Update> updates = byId(plan);
        assertThat(updates.get(12020L).name()).isEqualTo("Xã Đàm Thủy");
        assertThat(updates.get(12020L).slug()).isEqualTo("xa-dam-thuy-01501-2025");
        assertThat(updates.get(12021L).name()).isEqualTo("Xã Chân Mây - Lăng Cô");
        assertThat(updates.get(12022L).name()).isEqualTo("Xã Lý Quốc");
        assertThat(updates.get(12022L).slug()).isEqualTo("xa-ly-quoc-01537-2025");
        assertThat(plan.renames()).anyMatch(s -> s.contains("Xã Vinh Quý") && s.contains("Xã Lý Quốc"));
        assertThat(plan.stats()).containsEntry("Xã mới: khớp theo mã (đổi tên)", 1);
    }

    @Test
    void themXaThieuVaBaoCaoXaKhongKhopKhongXoa() {
        Plan plan = LocationFixPlanner.plan(ROWS, PROVINCES, WARDS);
        assertThat(plan.inserts()).containsExactly(new Insert("Phường Hoàn Kiếm", "phuong-hoan-kiem-00070-2025", "ward", "00070", 11960L));
        assertThat(plan.unmatched()).anyMatch(s -> s.contains("12023") && s.contains("Xã Không Có"));
        assertThat(byId(plan)).doesNotContainKey(12023L);
    }

    @Test
    void chayLaiSauKhiSuaThiKhongConGiDeSua() {
        Plan first = LocationFixPlanner.plan(ROWS, PROVINCES, WARDS);
        Map<Long, Update> updates = byId(first);
        List<LocationRow> fixed = new java.util.ArrayList<>(
            ROWS.stream()
                .map(r -> {
                    Update u = updates.get(r.id());
                    return u == null ? r : new LocationRow(r.id(), u.name(), u.slug(), u.type(), u.code(), u.parentId());
                })
                .toList()
        );
        long nextId = 20000;
        for (Insert insert : first.inserts()) {
            fixed.add(new LocationRow(nextId++, insert.name(), insert.slug(), insert.type(), insert.code(), insert.parentId()));
        }
        Plan second = LocationFixPlanner.plan(fixed, PROVINCES, WARDS);
        assertThat(second.updates()).isEmpty();
        assertThat(second.inserts()).isEmpty();
    }

    @Test
    void slugVaKhoaTen() {
        assertThat(AdministrativeUnits.slugify("Xã Chân Mây – Lăng Cô")).isEqualTo("xa-chan-may-lang-co");
        assertThat(AdministrativeUnits.slugify("Phường Đống Đa")).isEqualTo("phuong-dong-da");
        assertThat(AdministrativeUnits.codeFromSlug("phuong-ba-dinh-00004-2025")).isEqualTo("00004");
        assertThat(AdministrativeUnits.codeFromSlug("huyen-ba-vi-271")).isEqualTo("271");
        assertThat(AdministrativeUnits.codeFromSlug("international")).isNull();
        assertThat(AdministrativeUnits.nameKey("Xã Đàm Thuỷ")).isEqualTo(AdministrativeUnits.nameKey("xã  Đàm Thủy"));
        assertThat(AdministrativeUnits.nameKey("Bàn")).isNotEqualTo(AdministrativeUnits.nameKey("Bán"));
    }
}
