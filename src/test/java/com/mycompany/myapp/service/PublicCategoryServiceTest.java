package com.mycompany.myapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycompany.myapp.service.dto.publicapi.PublicCategoryDTO;
import com.mycompany.myapp.service.dto.publicapi.PublicPageDTO;
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
}
