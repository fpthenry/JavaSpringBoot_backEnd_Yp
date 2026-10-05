package com.mycompany.myapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.Gallery;
import com.mycompany.myapp.repository.GalleryAdminRepository;
import com.mycompany.myapp.repository.GalleryImageAdminRow;
import com.mycompany.myapp.service.dto.GalleryWithImagesDTO;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class GalleryAdminServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-05T08:00:00Z");

    private static Gallery gallery(long id, String code, boolean active) {
        Gallery g = new Gallery().name(code).code(code).active(active);
        g.setId(id);
        return g;
    }

    private static GalleryImageAdminRow image(
        long id,
        long galleryId,
        boolean uploaded,
        String url,
        boolean active,
        Instant start,
        Instant end
    ) {
        return new GalleryImageAdminRow(
            id,
            galleryId,
            "img" + id,
            uploaded,
            uploaded ? "image/png" : null,
            url,
            null,
            null,
            (int) id,
            active,
            start,
            end,
            false
        );
    }

    @Test
    void returnsEveryGalleryWithEveryImageAndShowingNowFlag() {
        GalleryAdminRepository repository = mock(GalleryAdminRepository.class);
        when(repository.findAllGalleries()).thenReturn(List.of(gallery(1, "a", true), gallery(2, "b", false), gallery(3, "empty", true)));
        when(repository.findAllImages()).thenReturn(
            List.of(
                image(10, 1, true, null, true, null, null), // đang hiện, ảnh upload
                image(11, 1, false, "https://x/1.jpg", false, null, null), // ảnh tắt
                image(12, 1, false, "https://x/2.jpg", true, null, NOW.minusSeconds(1)), // hết hạn
                image(13, 1, false, "https://x/3.jpg", true, NOW.plusSeconds(60), null), // chưa tới giờ
                image(14, 1, false, null, true, null, null), // không có ảnh
                image(20, 2, false, "https://x/4.jpg", true, null, null) // vị trí đang tắt
            )
        );
        var service = new GalleryAdminService(repository, Clock.fixed(NOW, ZoneOffset.UTC));

        List<GalleryWithImagesDTO> result = service.findAllWithImages(id -> "http://host/media/" + id);

        assertThat(result).extracting(GalleryWithImagesDTO::code).containsExactly("a", "b", "empty");
        assertThat(result).extracting(GalleryWithImagesDTO::imageCount).containsExactly(5, 1, 0);
        GalleryWithImagesDTO a = result.get(0);
        assertThat(a.images()).extracting(GalleryWithImagesDTO.Image::showingNow).containsExactly(true, false, false, false, false);
        assertThat(a.images().get(0).previewUrl()).isEqualTo("http://host/media/10");
        assertThat(a.images().get(1).previewUrl()).isEqualTo("https://x/1.jpg");
        assertThat(result.get(1).images().get(0).showingNow()).isFalse();
    }
}
