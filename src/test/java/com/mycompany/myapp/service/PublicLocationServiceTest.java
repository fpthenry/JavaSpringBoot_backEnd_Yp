package com.mycompany.myapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycompany.myapp.service.PublicLocationService.Row;
import com.mycompany.myapp.service.dto.publicapi.PublicLocationDTO;
import com.mycompany.myapp.service.dto.publicapi.PublicPageDTO;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

class PublicLocationServiceTest {

    // Bộ mới: Hà Nội 11960 > Vĩnh Tuy 12000, Đông Anh 12001; Bến Tre 13000 > Châu Thới 13001; Vĩnh Long > Châu Thới 14001.
    // Bộ cũ: Hà Nội 2 > Hai Bà Trưng 30 > Thanh Lương 800. Kết quả luôn theo tên riêng (Hai Bà Trưng trước Hà Nội).
    private static final List<Row> ROWS = List.of(
        new Row(11960, "Thành phố Hà Nội", "thanh-pho-ha-noi-01-2025", "province", null),
        new Row(12000, "Phường Vĩnh Tuy", "phuong-vinh-tuy-00283-2025", "ward", 11960L),
        new Row(12001, "Xã Đông Anh", "xa-dong-anh-00454-2025", "ward", 11960L),
        new Row(13000, "Tỉnh Bến Tre", "tinh-ben-tre-83-2025", "province", null),
        new Row(13001, "Xã Châu Thới", "xa-chau-thoi-29001-2025", "ward", 13000L),
        new Row(14000, "Tỉnh Vĩnh Long", "tinh-vinh-long-86-2025", "province", null),
        new Row(14001, "Xã Châu Thới", "xa-chau-thoi-29002-2025", "ward", 14000L),
        new Row(2, "Thành phố Hà Nội", "thanh-pho-ha-noi-01", "province", null),
        new Row(30, "Quận Hai Bà Trưng", "quan-hai-ba-trung-007", "district", 2L),
        new Row(800, "Phường Thanh Lương", "phuong-thanh-luong-00283", "ward", 30L)
    );
    private static final List<PublicLocationDTO> ALL = PublicLocationService.toDtos(
        ROWS,
        Map.of(11960L, 271_041L, 12000L, 2_375L, 13001L, 5L, 2L, 271_128L, 800L, 2_013L)
    );

    private static PublicLocationDTO byId(long id) {
        return ALL.stream()
            .filter(l -> l.id() == id)
            .findFirst()
            .orElseThrow();
    }

    @Test
    void chuCaiTheoTenRiengGiuDRieng() {
        assertThat(
            Stream.of(
                "Xã Châu Thới",
                "Phường Ô Chợ Dừa",
                "Thành phố Hà Nội",
                "Thị trấn Ưu Điềm",
                "Đặc khu Côn Đảo",
                "Xã Đông Anh",
                "đ",
                "Xã",
                ""
            ).map(PublicLocationService::letterOf)
        ).containsExactly("C", "O", "H", "U", "C", "Đ", "Đ", "X", "#");
        assertThat(PublicLocationService.properName("Thị xã Sơn Tây")).isEqualTo("Sơn Tây");
        assertThat(PublicLocationService.properName("Thành phố Thủ Đức")).isEqualTo("Thủ Đức");
    }

    @Test
    void dtoCoTinhChaVaSoDoanhNghiep() {
        PublicLocationDTO thanhLuong = byId(800);
        assertThat(thanhLuong.parentName()).isEqualTo("Quận Hai Bà Trưng");
        assertThat(thanhLuong.provinceName()).isEqualTo("Thành phố Hà Nội");
        assertThat(thanhLuong.isNew()).isFalse();
        assertThat(thanhLuong.listingCount()).isEqualTo(2_013L);
        PublicLocationDTO haNoi = byId(11960);
        assertThat(haNoi.isNew()).isTrue();
        assertThat(haNoi.provinceName()).isEqualTo("Thành phố Hà Nội");
        assertThat(haNoi.parentName()).isNull();
        assertThat(byId(12001).listingCount()).isZero();
    }

    @Test
    void mucLucChuCaiDSauDVaLocTheoDiaGioiCap() {
        List<PublicLocationDTO> moi = PublicLocationService.scope(ALL, "new", null, false);
        assertThat(PublicLocationService.letters(moi)).containsExactly(
            new PublicLocationDTO.Letter("B", 1),
            new PublicLocationDTO.Letter("C", 2),
            new PublicLocationDTO.Letter("Đ", 1),
            new PublicLocationDTO.Letter("H", 1),
            new PublicLocationDTO.Letter("V", 2)
        );
        assertThat(PublicLocationService.scope(ALL, "old", null, false))
            .extracting(PublicLocationDTO::id)
            .containsExactly(30L, 2L, 800L);
        assertThat(PublicLocationService.scope(ALL, "all", "ward", true))
            .extracting(PublicLocationDTO::id)
            .containsExactly(13001L, 800L, 12000L);
        assertThat(PublicLocationService.scope(ALL, "new", "province", false)).hasSize(3);
    }

    @Test
    void locChuCaiTuKhoaSapXepPhanTrang() {
        List<PublicLocationDTO> moi = PublicLocationService.scope(ALL, "new", null, false);
        PublicPageDTO<PublicLocationDTO> c = PublicLocationService.filter(moi, "c", null, PageRequest.of(0, 20));
        // Hai xã Châu Thới trùng tên: phân biệt bằng tỉnh, Bến Tre trước Vĩnh Long
        assertThat(c.items()).extracting(PublicLocationDTO::provinceName).containsExactly("Tỉnh Bến Tre", "Tỉnh Vĩnh Long");
        assertThat(PublicLocationService.filter(moi, "Đ", null, PageRequest.of(0, 20)).items())
            .extracting(PublicLocationDTO::id)
            .containsExactly(12001L);
        assertThat(PublicLocationService.filter(moi, "D", null, PageRequest.of(0, 20)).items()).isEmpty();
        // Từ khóa không dấu, tìm cả trong tên tỉnh
        assertThat(PublicLocationService.filter(moi, null, "chau thoi vinh long", PageRequest.of(0, 20)).items())
            .extracting(PublicLocationDTO::id)
            .containsExactly(14001L);
        assertThat(PublicLocationService.filter(moi, null, "HÀ NỘI", PageRequest.of(0, 20)).totalItems()).isEqualTo(3);
        // Từ khóa khớp đầu từ, không khớp giữa từ: "anh" không khớp "Thạnh", "ong" không khớp "Đông"
        assertThat(PublicLocationService.filter(moi, null, "dong a", PageRequest.of(0, 20)).items())
            .extracting(PublicLocationDTO::id)
            .containsExactly(12001L);
        assertThat(PublicLocationService.filter(moi, null, "ong", PageRequest.of(0, 20)).items()).isEmpty();
        // Khớp trong tên đơn vị xếp trước khớp nhờ tên tỉnh: Tỉnh Vĩnh Long trước Xã Châu Thới (Vĩnh Long), kể cả khi sort=name
        assertThat(PublicLocationService.filter(moi, null, "vinh long", PageRequest.of(0, 20)).items())
            .extracting(PublicLocationDTO::id)
            .containsExactly(14000L, 14001L);
        assertThat(PublicLocationService.filter(moi, null, "vinh long", PageRequest.of(0, 20, Sort.by("name"))).items())
            .extracting(PublicLocationDTO::id)
            .containsExactly(14000L, 14001L);
        // Sắp theo số doanh nghiệp: không ưu tiên khớp trong tên (cả hai 0 doanh nghiệp nên giữ thứ tự tên: Châu Thới trước)
        assertThat(
            PublicLocationService.filter(moi, null, "vinh long", PageRequest.of(0, 20, Sort.by(Sort.Order.desc("listingCount")))).items()
        )
            .extracting(PublicLocationDTO::id)
            .containsExactly(14001L, 14000L);
        // Mặc định theo tên riêng: Bến Tre, Châu Thới, Châu Thới, Đông Anh, Hà Nội, Vĩnh Long, Vĩnh Tuy
        assertThat(PublicLocationService.filter(moi, null, null, PageRequest.of(0, 20)).items())
            .extracting(PublicLocationDTO::id)
            .containsExactly(13000L, 13001L, 14001L, 12001L, 11960L, 14000L, 12000L);
        PublicPageDTO<PublicLocationDTO> top = PublicLocationService.filter(
            moi,
            null,
            null,
            PageRequest.of(0, 2, Sort.by(Sort.Order.desc("listingCount")))
        );
        assertThat(top.items()).extracting(PublicLocationDTO::id).containsExactly(11960L, 12000L);
        assertThat(top.totalPages()).isEqualTo(4);
        assertThat(PublicLocationService.filter(moi, null, null, PageRequest.of(9, 2)).items()).isEmpty();
    }

    @Test
    void tenBatDauBangSoXepCuoiNhuNutThang() {
        List<PublicLocationDTO> list = PublicLocationService.toDtos(
            List.of(
                new Row(1, "Phường 1", "phuong-1-00001", "ward", 3L),
                new Row(2, "Phường Ái Quốc", "phuong-ai-quoc-00002", "ward", 3L),
                new Row(3, "Tỉnh Lâm Đồng", "tinh-lam-dong-68", "province", null),
                new Row(4, "Xã Đinh Lạc", "xa-dinh-lac-00004", "ward", 3L),
                new Row(5, "Xã Dương Hòa", "xa-duong-hoa-00005", "ward", 3L)
            ),
            Map.of()
        );
        assertThat(list).extracting(PublicLocationDTO::id).containsExactly(2L, 5L, 4L, 3L, 1L);
        assertThat(list.get(4).letter()).isEqualTo("#");
    }

    @Test
    void thamSoSaiTraVe400() {
        assertThatThrownBy(() -> PublicLocationService.checkedLetter("ab")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> PublicLocationService.checkedLetter("1")).isInstanceOf(ResponseStatusException.class);
        assertThat(PublicLocationService.checkedLetter("#")).isEqualTo("#");
        assertThat(PublicLocationService.checkedLetter("ô")).isEqualTo("O");
        assertThatThrownBy(() -> PublicLocationService.checkedGeneration("2025")).isInstanceOf(ResponseStatusException.class);
        assertThat(PublicLocationService.checkedGeneration(null)).isEqualTo("new");
        assertThat(PublicLocationService.checkedGeneration(" ALL ")).isEqualTo("all");
        assertThatThrownBy(() -> PublicLocationService.checkedLevel("commune")).isInstanceOf(ResponseStatusException.class);
        assertThat(PublicLocationService.checkedLevel("")).isNull();
        List<PublicLocationDTO> moi = PublicLocationService.scope(ALL, "new", null, false);
        assertThatThrownBy(() -> PublicLocationService.filter(moi, null, null, PageRequest.of(0, 20, Sort.by("slug")))).isInstanceOf(
            ResponseStatusException.class
        );
    }
}
