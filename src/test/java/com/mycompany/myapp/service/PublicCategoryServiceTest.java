package com.mycompany.myapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycompany.myapp.service.dto.publicapi.PublicCategoryDTO;
import com.mycompany.myapp.service.dto.publicapi.PublicPageDTO;
import com.mycompany.myapp.service.dto.publicapi.PublicTermDetailDTO;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

class PublicCategoryServiceTest {

    private static PublicCategoryDTO row(long id, String name, Long parentId, String parentName, Long count) {
        return PublicCategoryService.toDto(new Object[] { id, name, "slug-" + id, parentId, parentName, count });
    }

    private static final List<PublicCategoryDTO> ALL = List.of(
        row(1, "Lắp đặt hệ thống điện", 10L, "Lắp đặt", 352_693L),
        row(2, "LỊCH", null, null, 51L),
        row(3, "Lập trình máy vi tính", 11L, "Dịch vụ máy tính", 74L),
        row(4, "LEN", null, null, 0L),
        row(5, "Điện tử", null, null, 9L),
        row(6, "Ống nước", null, null, 3L),
        row(7, "LỌC - THIẾT BỊ LỌC KHÔNG KHÍ - GA &amp; CHẤT LỎNG", null, null, null),
        row(8, "3D - in ấn", null, null, 2L)
    );

    @Test
    void chuCaiDauBoDauVaTenGiaiMa() {
        assertThat(Stream.of("Lắp", "Đá", "ống", "Ưu", "ăn", "zeta", "3D", " ", "").map(PublicCategoryService::letterOf)).containsExactly(
            "L",
            "D",
            "O",
            "U",
            "A",
            "Z",
            "#",
            "#",
            "#"
        );
        assertThat(ALL.get(6).name()).isEqualTo("LỌC - THIẾT BỊ LỌC KHÔNG KHÍ - GA & CHẤT LỎNG");
        assertThat(ALL.get(6).listingCount()).isZero();
    }

    @Test
    void mucLucChuCaiAzRoiDenThang() {
        assertThat(PublicCategoryService.letters(ALL, false)).containsExactly(
            new PublicCategoryDTO.Letter("D", 1),
            new PublicCategoryDTO.Letter("L", 5),
            new PublicCategoryDTO.Letter("O", 1),
            new PublicCategoryDTO.Letter("#", 1)
        );
        assertThat(PublicCategoryService.letters(ALL, true)).extracting(PublicCategoryDTO.Letter::count).containsExactly(1L, 3L, 1L, 1L);
    }

    @Test
    void locTheoChuCaiSapTheoTenTiengViet() {
        PublicPageDTO<PublicCategoryDTO> page = PublicCategoryService.filter(ALL, "l", null, false, PageRequest.of(0, 20));
        assertThat(page.items()).extracting(PublicCategoryDTO::id).containsExactly(1L, 3L, 4L, 2L, 7L);
        assertThat(page.totalItems()).isEqualTo(5);
        assertThat(PublicCategoryService.filter(ALL, "đ", null, false, PageRequest.of(0, 20)).items())
            .extracting(PublicCategoryDTO::id)
            .containsExactly(5L);
        assertThat(PublicCategoryService.filter(ALL, "#", null, false, PageRequest.of(0, 20)).items())
            .extracting(PublicCategoryDTO::id)
            .containsExactly(8L);
    }

    @Test
    void timKhongDauAnNganhTrongSapXepVaPhanTrang() {
        assertThat(PublicCategoryService.filter(ALL, null, "lap DAT dien", false, PageRequest.of(0, 20)).items())
            .extracting(PublicCategoryDTO::id)
            .containsExactly(1L);
        PublicPageDTO<PublicCategoryDTO> page = PublicCategoryService.filter(
            ALL,
            "L",
            null,
            true,
            PageRequest.of(1, 2, Sort.by(Sort.Order.desc("listingCount")))
        );
        assertThat(page.items()).extracting(PublicCategoryDTO::id).containsExactly(2L);
        assertThat(page.totalItems()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(PublicCategoryService.filter(ALL, null, null, false, PageRequest.of(9, 20)).items()).isEmpty();
    }

    @Test
    void thamSoSaiThi400() {
        assertThatThrownBy(() -> PublicCategoryService.filter(ALL, "LA", null, false, PageRequest.of(0, 20))).isInstanceOf(
            ResponseStatusException.class
        );
        assertThatThrownBy(() -> PublicCategoryService.filter(ALL, "1", null, false, PageRequest.of(0, 20))).isInstanceOf(
            ResponseStatusException.class
        );
        assertThatThrownBy(() -> PublicCategoryService.filter(ALL, null, null, false, PageRequest.of(0, 20, Sort.by("slug"))))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("slug");
    }

    @Test
    void timTheoSlugHoacId() {
        assertThat(PublicCategoryService.find(ALL, "slug-3")).map(PublicCategoryDTO::id).contains(3L);
        assertThat(PublicCategoryService.find(ALL, " 5 ")).map(PublicCategoryDTO::name).contains("Điện tử");
        assertThat(PublicCategoryService.find(ALL, "khong-co")).isEmpty();
        assertThat(PublicCategoryService.find(ALL, "99999999999999999999")).isEmpty();
        assertThat(PublicCategoryService.find(ALL, " ")).isEmpty();
    }

    @Test
    void chiTietCoBreadcrumbVaNganhCon() {
        List<PublicCategoryDTO> tree = List.of(
            row(10, "Xây dựng", null, null, 100L),
            row(11, "Lắp đặt", 10L, "Xây dựng", 60L),
            row(12, "Lắp đặt hệ thống điện", 11L, "Lắp đặt", 40L),
            row(13, "Lắp đặt cấp thoát nước", 11L, "Lắp đặt", 20L)
        );
        PublicTermDetailDTO detail = PublicCategoryService.detail(tree, tree.get(1), "  Mô tả  ");
        assertThat(detail.description()).isEqualTo("Mô tả");
        assertThat(detail.ancestors()).extracting(PublicTermDetailDTO.Ref::id).containsExactly(10L);
        assertThat(detail.children())
            .extracting(PublicTermDetailDTO.Ref::name)
            .containsExactly("Lắp đặt cấp thoát nước", "Lắp đặt hệ thống điện");
        assertThat(PublicCategoryService.detail(tree, tree.get(2), null).ancestors())
            .extracting(PublicTermDetailDTO.Ref::id)
            .containsExactly(10L, 11L);
        assertThat(PublicCategoryService.detail(tree, tree.get(2), " ").description()).isNull();
    }

    @Test
    void locNganhGocNganhConVaSoDoanhNghiep() {
        PageRequest page = PageRequest.of(0, 20);
        assertThat(PublicCategoryService.filter(ALL, null, null, 0, PublicCategoryService.parentFilter(ALL, "root"), page).items())
            .extracting(PublicCategoryDTO::id)
            .doesNotContain(1L, 3L)
            .contains(2L, 5L);
        assertThat(
            PublicCategoryService.filter(ALL, null, null, 0, PublicCategoryService.parentFilter(ALL, "slug-2"), page).items()
        ).isEmpty();
        assertThat(PublicCategoryService.filter(ALL, null, null, 51, PublicCategoryService.parentFilter(ALL, null), page).items())
            .extracting(PublicCategoryDTO::id)
            .containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThatThrownBy(() -> PublicCategoryService.parentFilter(ALL, "khong-co")).isInstanceOf(ResponseStatusException.class);
    }
}
