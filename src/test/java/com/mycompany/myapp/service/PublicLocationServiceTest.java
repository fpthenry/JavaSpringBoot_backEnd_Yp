package com.mycompany.myapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.mycompany.myapp.service.PublicLocationService.Conversion;
import com.mycompany.myapp.service.PublicLocationService.Row;
import com.mycompany.myapp.service.dto.publicapi.PublicLocationDTO;
import com.mycompany.myapp.service.dto.publicapi.PublicPageDTO;
import com.mycompany.myapp.service.dto.publicapi.PublicTermDetailDTO;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.LongFunction;
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

    @Test
    void slugXaDaDoiTenTimTheoMaRaSlugMoi() {
        // Xã lên phường (NQ 2026): slug cũ "xa-...-00454-2025" → đơn vị bộ mới cùng mã
        assertThat(PublicLocationService.findNewByCode(ALL, "xa-vinh-tuy-00283-2025")).map(PublicLocationDTO::id).contains(12000L);
        assertThat(PublicLocationService.findNewByCode(ALL, "phuong-dong-anh-00454-2025"))
            .map(PublicLocationDTO::slug)
            .contains("xa-dong-anh-00454-2025");
        // Bộ cũ và mã không có: không đoán
        assertThat(PublicLocationService.findNewByCode(ALL, "phuong-thanh-luong-00283")).isEmpty();
        assertThat(PublicLocationService.findNewByCode(ALL, "xa-khong-co-99999-2025")).isEmpty();
        assertThat(PublicLocationService.findNewByCode(ALL, " ")).isEmpty();
    }

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

    @Test
    void chiTietCoBreadcrumbVaDonViCon() {
        PublicTermDetailDTO ward = PublicLocationService.detail(ALL, byId(800));
        assertThat(ward.ancestors()).extracting(PublicTermDetailDTO.Ref::name).containsExactly("Thành phố Hà Nội", "Quận Hai Bà Trưng");
        assertThat(ward.type()).isEqualTo("ward");
        assertThat(ward.isNew()).isFalse();
        PublicTermDetailDTO hanoi = PublicLocationService.detail(ALL, byId(11960));
        assertThat(hanoi.ancestors()).isEmpty();
        assertThat(hanoi.listingCount()).isEqualTo(271_041L);
        // Giữ thứ tự theo tên riêng: Đ đứng sau D, trước V nên Đông Anh trước Vĩnh Tuy
        assertThat(hanoi.children()).extracting(PublicTermDetailDTO.Ref::id).containsExactly(12001L, 12000L);
    }

    @Test
    void timTheoSlugHoacId() {
        assertThat(PublicLocationService.find(ALL, "xa-chau-thoi-29002-2025")).map(PublicLocationDTO::id).contains(14001L);
        assertThat(PublicLocationService.find(ALL, "13001")).map(PublicLocationDTO::slug).contains("xa-chau-thoi-29001-2025");
        assertThat(PublicLocationService.find(ALL, "khong-co")).isEmpty();
    }

    // Thanh Lương cũ → Vĩnh Tuy; Vĩnh Tuy cũ bị chia (Vĩnh Tuy, Đông Anh); cả Huyện Đông Anh (mã xã trống) → Xã Đông Anh
    private static final List<Conversion> CONVERSIONS = List.of(
        new Conversion("00283", "Phường Thanh Lương", "007", "Quận Hai Bà Trưng", "01", "Thành phố Hà Nội", "01", "00283", false),
        new Conversion("00290", "Phường Vĩnh Tuy", "007", "Quận Hai Bà Trưng", "01", "Thành phố Hà Nội", "01", "00454", true),
        new Conversion("00290", "Phường Vĩnh Tuy", "007", "Quận Hai Bà Trưng", "01", "Thành phố Hà Nội", "01", "00283", true),
        new Conversion(null, null, "017", "Huyện Đông Anh", "01", "Thành phố Hà Nội", "01", "00454", false)
    );
    private static final BiFunction<PublicLocationDTO, List<PublicLocationDTO>, Map<Long, Long>> NO_SHARED = (old, targets) -> Map.of();

    @Test
    void donViCuRaDonViMoiChuaNo() {
        List<PublicLocationDTO> all = PublicLocationService.toDtos(
            Stream.concat(
                ROWS.stream(),
                Stream.of(
                    new Row(801, "Phường Vĩnh Tuy", "phuong-vinh-tuy-00290", "ward", 30L),
                    new Row(802, "Phường Không Có Trong Bảng", "phuong-khong-co-00999", "ward", 30L),
                    new Row(803, "Phường Lạc Trung", "phuong-lac-trung-00291", "ward", 30L)
                )
            ).toList(),
            Map.of()
        );
        // Lạc Trung cũ bị chia (Đông Anh trước theo bảng, Vĩnh Tuy sau), không xã mới nào cùng tên
        List<Conversion> split = Stream.concat(
            CONVERSIONS.stream(),
            Stream.of(
                new Conversion("00291", "Phường Lạc Trung", "007", "Quận Hai Bà Trưng", "01", "Thành phố Hà Nội", "01", "00454", true),
                new Conversion("00291", "Phường Lạc Trung", "007", "Quận Hai Bà Trưng", "01", "Thành phố Hà Nội", "01", "00283", true)
            )
        ).toList();
        LongFunction<PublicLocationDTO> id = n ->
            all
                .stream()
                .filter(l -> l.id() == n)
                .findFirst()
                .orElseThrow();
        // Xã cũ → xã mới; bị chia thì xã mới cùng tên riêng
        assertThat(PublicLocationService.newUnitOf(all, CONVERSIONS, id.apply(800), NO_SHARED))
            .map(PublicLocationDTO::id)
            .contains(12000L);
        assertThat(PublicLocationService.newUnitOf(all, CONVERSIONS, id.apply(801), NO_SHARED))
            .map(PublicLocationDTO::id)
            .contains(12000L);
        // Quận/huyện, tỉnh cũ → tỉnh mới; xã không có trong bảng → theo huyện chứa nó
        assertThat(PublicLocationService.newUnitOf(all, CONVERSIONS, id.apply(30), NO_SHARED))
            .map(PublicLocationDTO::id)
            .contains(11960L);
        assertThat(PublicLocationService.newUnitOf(all, CONVERSIONS, id.apply(2), NO_SHARED))
            .map(PublicLocationDTO::id)
            .contains(11960L);
        assertThat(PublicLocationService.newUnitOf(all, CONVERSIONS, id.apply(802), NO_SHARED))
            .map(PublicLocationDTO::id)
            .contains(11960L);
        // Bị chia, không cùng tên: nơi có nhiều doanh nghiệp của xã cũ nhất; không có số liệu thì theo bảng
        assertThat(PublicLocationService.newUnitOf(all, split, id.apply(803), NO_SHARED))
            .map(PublicLocationDTO::id)
            .contains(12001L);
        assertThat(PublicLocationService.newUnitOf(all, split, id.apply(803), (old, targets) -> Map.of(12000L, 50L, 12001L, 3L)))
            .map(PublicLocationDTO::id)
            .contains(12000L);
    }

    @Test
    void timTheoTenCuKemDonViMoi() {
        List<PublicLocationDTO.Former> thanhLuong = PublicLocationService.former(ALL, CONVERSIONS, "thanh luong", 10);
        assertThat(thanhLuong).extracting(PublicLocationDTO.Former::name).containsExactly("Phường Thanh Lương");
        assertThat(thanhLuong.getFirst().parentName()).isEqualTo("Quận Hai Bà Trưng");
        assertThat(thanhLuong.getFirst().targets()).extracting(PublicLocationDTO::id).containsExactly(12000L);
        // Huyện cũ → các xã mới, nhiều doanh nghiệp trước (Vĩnh Tuy 2.375 trước Đông Anh 0)
        List<PublicLocationDTO.Former> haiBaTrung = PublicLocationService.former(ALL, CONVERSIONS, "Hai Bà Trưng", 10);
        assertThat(haiBaTrung).extracting(PublicLocationDTO.Former::type).containsExactly("district");
        assertThat(haiBaTrung.getFirst().targets()).extracting(PublicLocationDTO::id).containsExactly(12000L, 12001L);
        // Xã bị chia: giữ dù cùng tên với một xã mới
        assertThat(PublicLocationService.former(ALL, CONVERSIONS, "vinh tuy", 10))
            .singleElement()
            .satisfies(f -> assertThat(f.targets()).hasSize(2));
        // Chỉ đổi cấp, giữ tên riêng (Huyện Đông Anh → Xã Đông Anh, Hà Nội → Hà Nội): tìm thường đã ra, bỏ
        assertThat(PublicLocationService.former(ALL, CONVERSIONS, "dong anh", 10)).isEmpty();
        assertThat(PublicLocationService.former(ALL, CONVERSIONS, "ha noi", 10)).isEmpty();
        assertThat(PublicLocationService.former(ALL, CONVERSIONS, " ", 10)).isEmpty();
    }

    @Test
    void donViMoiGomCacDonViCu() {
        assertThat(PublicLocationService.origins(ALL, CONVERSIONS, byId(12000)))
            .extracting(PublicLocationDTO.Origin::name, PublicLocationDTO.Origin::partial)
            .containsExactly(tuple("Phường Thanh Lương", false), tuple("Phường Vĩnh Tuy", true));
        assertThat(PublicLocationService.origins(ALL, CONVERSIONS, byId(12001)))
            .extracting(PublicLocationDTO.Origin::name, PublicLocationDTO.Origin::type)
            .containsExactly(tuple("Phường Vĩnh Tuy", "ward"), tuple("Huyện Đông Anh", "district"));
        assertThat(PublicLocationService.origins(ALL, CONVERSIONS, byId(11960)))
            .extracting(PublicLocationDTO.Origin::name)
            .containsExactly("Thành phố Hà Nội");
        assertThat(PublicLocationService.origins(ALL, CONVERSIONS, byId(800))).isEmpty();
        // Kèm id đơn vị cũ trong bảng location (Thanh Lương 800); xã cũ chưa từng có trên website: id null
        assertThat(PublicLocationService.origins(ALL, CONVERSIONS, byId(12000)))
            .extracting(PublicLocationDTO.Origin::id, PublicLocationDTO.Origin::code)
            .containsExactly(tuple(800L, "00283"), tuple(null, "00290"));
        assertThat(PublicLocationService.origins(ALL, CONVERSIONS, byId(11960)).getFirst().id()).isEqualTo(2L);
    }

    @Test
    void donViCuNayThuocDau() {
        assertThat(PublicLocationService.successors(ALL, CONVERSIONS, byId(800)))
            .extracting(PublicLocationDTO::id)
            .containsExactly(12000L);
        // Quận cũ → các xã mới, nơi nhận nhiều xã cũ nhất trước (Vĩnh Tuy nhận 2, Đông Anh 1)
        assertThat(PublicLocationService.successors(ALL, CONVERSIONS, byId(30)))
            .extracting(PublicLocationDTO::id)
            .containsExactly(12000L, 12001L);
        assertThat(PublicLocationService.successors(ALL, CONVERSIONS, byId(2)))
            .extracting(PublicLocationDTO::id)
            .containsExactly(11960L);
        assertThat(PublicLocationService.successors(ALL, CONVERSIONS, byId(12000))).isEmpty();
    }

    @Test
    void nutCayTachBoMoiBoCuCoCoChuyenDoi() {
        assertThat(PublicLocationService.nodes(ALL, CONVERSIONS, "old"))
            .extracting(PublicLocationDTO.Node::id, PublicLocationDTO.Node::code, PublicLocationDTO.Node::mapped)
            .containsExactlyInAnyOrder(tuple(2L, "01", true), tuple(30L, "007", true), tuple(800L, "00283", true));
        List<PublicLocationDTO.Node> moi = PublicLocationService.nodes(ALL, CONVERSIONS, "new");
        assertThat(moi)
            .hasSize(7)
            .allMatch(n -> n.code() != null);
        // Bến Tre, Vĩnh Long không có trong bảng chuyển đổi mẫu
        assertThat(moi)
            .filteredOn(PublicLocationDTO.Node::mapped)
            .extracting(PublicLocationDTO.Node::id)
            .containsExactlyInAnyOrder(11960L, 12000L, 12001L);
    }

    @Test
    void docBangSlugCu() {
        assertThat(
            PublicLocationService.parseSlugAliases(
                List.of("# chú thích", "old_slug,wp_term_id", "phuong-cua-nam-00073-2025,80064", "", "hong,abc", " xa-a-1-2025 , 7 ")
            )
        ).containsOnly(Map.entry("phuong-cua-nam-00073-2025", 80064L), Map.entry("xa-a-1-2025", 7L));
    }
}
