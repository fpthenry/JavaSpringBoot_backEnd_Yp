package com.mycompany.myapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

class PublicListingServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void toaDoGiuNguyenVan() {
        assertThat(
            PublicListingService.coordinate("{\"province\":\"Thành phố Hà Nội\",\"ward\":null,\"coordinate\":\"21.0001000:105.698\"}")
        ).isEqualTo("21.0001000:105.698");
        assertThat(PublicListingService.coordinate("{\"province\":\"Hà Nội\"}")).isNull();
        assertThat(PublicListingService.coordinate("{\"coordinate\":\"\"}")).isNull();
        assertThat(PublicListingService.coordinate(null)).isNull();
    }

    @Test
    void anhChiNhanUrl() {
        assertThat(PublicListingService.imageUrl(" https://yp.vn/a.jpg ")).isEqualTo("https://yp.vn/a.jpg");
        assertThat(PublicListingService.imageUrl("6214992")).isNull();
        assertThat(PublicListingService.imageUrl("")).isNull();
    }

    @Test
    void thuVienAnhNhieuDang() {
        assertThat(PublicListingService.imageUrls("6229172", MAPPER)).isEmpty();
        assertThat(PublicListingService.imageUrls("6229172,https://yp.vn/b.jpg", MAPPER)).containsExactly("https://yp.vn/b.jpg");
        assertThat(
            PublicListingService.imageUrls("[\"https://yp.vn/a.jpg\",\"https://yp.vn/b.jpg\",\"https://yp.vn/a.jpg\"]", MAPPER)
        ).containsExactly("https://yp.vn/a.jpg", "https://yp.vn/b.jpg");
        // Dạng gallery của WordPress, dấu / bị escape
        assertThat(
            PublicListingService.imageUrls(
                "[{\"image_url\":\"https:\\/\\/yp.vn\\/wp-content\\/uploads\\/c.jpg\",\"caption\":\"\"},{\"caption\":\"x\"}]",
                MAPPER
            )
        ).containsExactly("https://yp.vn/wp-content/uploads/c.jpg");
        assertThat(PublicListingService.imageUrls("[hỏng", MAPPER)).isEmpty();
    }

    @Test
    void sapXepMacDinhTheoTuKhoa() {
        assertThat(PublicListingService.esSort(Sort.unsorted(), false)).containsExactly(
            Sort.Order.desc("publishedAt"),
            Sort.Order.desc("sortId")
        );
        assertThat(PublicListingService.esSort(Sort.unsorted(), true)).containsExactly(
            Sort.Order.desc("_score"),
            Sort.Order.desc("sortId")
        );
        assertThat(PublicListingService.esSort(Sort.by("name"), true)).containsExactly(
            Sort.Order.asc("name.keyword"),
            Sort.Order.desc("sortId")
        );
        assertThatThrownBy(() -> PublicListingService.esSort(Sort.by("phone"), false)).isInstanceOf(ResponseStatusException.class);
    }
}
