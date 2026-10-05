package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.Gallery;
import com.mycompany.myapp.repository.GalleryAdminRepository;
import com.mycompany.myapp.repository.GalleryImageAdminRow;
import com.mycompany.myapp.service.dto.GalleryWithImagesDTO;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.LongFunction;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tất cả vị trí banner kèm mọi ảnh, cho quản trị xem toàn cảnh (dữ liệu banner ít, không phân trang).
 */
@Service
@Transactional(readOnly = true)
public class GalleryAdminService {

    private final GalleryAdminRepository repository;
    private final Clock clock;

    @Autowired
    public GalleryAdminService(GalleryAdminRepository repository) {
        this(repository, Clock.systemUTC());
    }

    GalleryAdminService(GalleryAdminRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * @param uploadedImageUrl dựng URL xem ảnh upload từ id ảnh (tầng web biết địa chỉ server).
     */
    public List<GalleryWithImagesDTO> findAllWithImages(LongFunction<String> uploadedImageUrl) {
        Instant now = clock.instant();
        Map<Long, List<GalleryImageAdminRow>> imagesByGallery = repository
            .findAllImages()
            .stream()
            .collect(Collectors.groupingBy(GalleryImageAdminRow::galleryId));
        return repository
            .findAllGalleries()
            .stream()
            .map(gallery -> {
                List<GalleryWithImagesDTO.Image> images = imagesByGallery
                    .getOrDefault(gallery.getId(), List.of())
                    .stream()
                    .map(row -> toImage(row, gallery, now, uploadedImageUrl))
                    .toList();
                return new GalleryWithImagesDTO(
                    gallery.getId(),
                    gallery.getName(),
                    gallery.getCode(),
                    gallery.getDescription(),
                    gallery.getActive(),
                    gallery.getWpId(),
                    images.size(),
                    images
                );
            })
            .toList();
    }

    private static GalleryWithImagesDTO.Image toImage(
        GalleryImageAdminRow row,
        Gallery gallery,
        Instant now,
        LongFunction<String> uploadedImageUrl
    ) {
        boolean hasImage = row.uploaded() || row.imageUrl() != null;
        // Cùng điều kiện với API công khai (GalleryPublicRepository.CURRENT_IMAGE)
        boolean showingNow =
            Boolean.TRUE.equals(row.active()) &&
            Boolean.TRUE.equals(gallery.getActive()) &&
            (row.startAt() == null || !row.startAt().isAfter(now)) &&
            (row.endAt() == null || row.endAt().isAfter(now)) &&
            hasImage;
        return new GalleryWithImagesDTO.Image(
            row.id(),
            row.title(),
            row.uploaded(),
            row.imageContentType(),
            row.uploaded() ? uploadedImageUrl.apply(row.id()) : row.imageUrl(),
            row.imageUrl(),
            row.linkUrl(),
            row.altText(),
            row.displayOrder(),
            row.active(),
            row.startAt(),
            row.endAt(),
            row.openInNewTab(),
            showingNow
        );
    }
}
