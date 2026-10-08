package com.mycompany.myapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycompany.myapp.service.PublicBlogService.CategoryRow;
import com.mycompany.myapp.service.dto.publicapi.PublicBlogCategoryDTO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

class PublicBlogServiceTest {

    // Tin tức (1) > Doanh nghiệp (2) > Ăn uống (3); Sự kiện (4); Trống (5); 8 <-> 9 trỏ vòng nhau (dữ liệu lỗi)
    private static final List<CategoryRow> ROWS = List.of(
        new CategoryRow(1L, null, "Tin tức", "tin-tuc", null),
        new CategoryRow(2L, 1L, "Doanh nghiệp", "doanh-nghiep", null),
        new CategoryRow(3L, 2L, "Ăn uống", "an-uong", null),
        new CategoryRow(4L, null, "Sự kiện", "su-kien", null),
        new CategoryRow(5L, 1L, "Trống", "trong", null),
        new CategoryRow(8L, 9L, "Vòng A", "vong-a", null),
        new CategoryRow(9L, 8L, "Vòng B", "vong-b", null)
    );

    // Bài 100 gắn cả Tin tức và Ăn uống (chỉ đếm 1 lần ở Tin tức); bài 101 ở Doanh nghiệp; bài 102 ở Sự kiện
    private static final List<Object[]> PAIRS = List.of(
        new Object[] { 100L, 1L },
        new Object[] { 100L, 3L },
        new Object[] { 101L, 2L },
        new Object[] { 102L, 4L }
    );

    @Test
    void dungCayDemBaiTrucTiepVaCaCayConMoiBaiMotLan() {
        List<PublicBlogCategoryDTO> tree = PublicBlogService.buildTree(ROWS, PAIRS, false);

        // Gốc sắp theo tên tiếng Việt; danh mục trong vòng lặp được đưa lên gốc, không bị mất
        assertThat(tree).extracting(PublicBlogCategoryDTO::name).containsExactly("Sự kiện", "Tin tức", "Vòng A", "Vòng B");
        PublicBlogCategoryDTO tinTuc = tree.get(1);
        assertThat(tinTuc.postCount()).isEqualTo(1);
        assertThat(tinTuc.totalPostCount()).isEqualTo(2);
        assertThat(tinTuc.children()).extracting(PublicBlogCategoryDTO::name).containsExactly("Doanh nghiệp", "Trống");
        PublicBlogCategoryDTO doanhNghiep = tinTuc.children().get(0);
        assertThat(doanhNghiep.totalPostCount()).isEqualTo(2);
        assertThat(doanhNghiep.children().get(0).slug()).isEqualTo("an-uong");
        assertThat(doanhNghiep.children().get(0).parentId()).isEqualTo(2L);
    }

    @Test
    void anDanhMucKhongCoBai() {
        List<PublicBlogCategoryDTO> tree = PublicBlogService.buildTree(ROWS, PAIRS, true);
        assertThat(tree).extracting(PublicBlogCategoryDTO::slug).containsExactly("su-kien", "tin-tuc");
        assertThat(tree.get(1).children()).extracting(PublicBlogCategoryDTO::slug).containsExactly("doanh-nghiep");
    }

    @Test
    void timDanhMucTheoSlugHoacIdVaBreadcrumb() {
        CategoryRow anUong = PublicBlogService.findRow(ROWS, "AN-UONG").orElseThrow();
        assertThat(PublicBlogService.findRow(ROWS, "3")).contains(anUong);
        assertThat(PublicBlogService.findRow(ROWS, "khong-co")).isEmpty();
        assertThat(PublicBlogService.findRow(ROWS, " ")).isEmpty();
        assertThat(PublicBlogService.breadcrumb(ROWS, anUong))
            .extracting(t -> t.slug())
            .containsExactly("tin-tuc", "doanh-nghiep");
        assertThat(PublicBlogService.breadcrumb(ROWS, PublicBlogService.findRow(ROWS, "vong-a").orElseThrow()))
            .extracting(t -> t.slug())
            .containsExactly("vong-b");
    }

    @Test
    void phanTrangGioiHanCoTrangVaChiChoSapXepFieldHopLe() {
        Pageable checked = PublicBlogService.checkedPageable(PageRequest.of(2, 500, Sort.by(Sort.Order.asc("publishedAt"))));
        assertThat(checked.getPageSize()).isEqualTo(PublicBlogService.MAX_PAGE_SIZE);
        assertThat(checked.getPageNumber()).isEqualTo(2);
        assertThat(checked.getSort().getOrderFor("publishedAt")).isNotNull();

        assertThatThrownBy(() -> PublicBlogService.checkedPageable(PageRequest.of(0, 10, Sort.by("title"))))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("title");
    }
}
