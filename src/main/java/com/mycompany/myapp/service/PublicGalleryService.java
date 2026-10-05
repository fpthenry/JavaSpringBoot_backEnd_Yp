package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.Gallery;
import com.mycompany.myapp.domain.GalleryImage;
import com.mycompany.myapp.repository.GalleryPublicRepository;
import com.mycompany.myapp.repository.GalleryPublicRow;
import com.mycompany.myapp.service.dto.publicapi.PublicGalleryDTO;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.LongFunction;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đọc gallery/banner cho API công khai: chỉ vị trí đang bật và ảnh đang chạy
 * (active, trong khoảng startAt-endAt, có ảnh upload hoặc link ảnh), theo displayOrder.
 */
@Service
@Transactional(readOnly = true)
public class PublicGalleryService {

    private final GalleryPublicRepository repository;
    private final Clock clock;

    @Autowired
    public PublicGalleryService(GalleryPublicRepository repository) {
        this(repository, Clock.systemUTC());
    }

    PublicGalleryService(GalleryPublicRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * @param codes             mã vị trí cần lấy; rỗng = mọi vị trí đang bật.
     * @param uploadedImageUrl  dựng URL ảnh upload từ id ảnh (do tầng web biết địa chỉ server).
     */
    public List<PublicGalleryDTO> findGalleries(Collection<String> codes, LongFunction<String> uploadedImageUrl) {
        Instant now = clock.instant();
        boolean all = codes == null || codes.isEmpty();
        List<Gallery> galleries = all ? repository.findAllActiveGalleries() : repository.findActiveGalleries(codes);
        List<GalleryPublicRow> rows = all ? repository.findAllCurrentImages(now) : repository.findCurrentImages(codes, now);
        Map<String, List<PublicGalleryDTO.Image>> imagesByCode = rows
            .stream()
            .collect(
                Collectors.groupingBy(
                    GalleryPublicRow::galleryCode,
                    LinkedHashMap::new,
                    Collectors.mapping(row -> toImage(row, uploadedImageUrl), Collectors.toList())
                )
            );
        return galleries
            .stream()
            .map(g -> new PublicGalleryDTO(g.getCode(), g.getName(), g.getDescription(), imagesByCode.getOrDefault(g.getCode(), List.of())))
            .toList();
    }

    public Optional<PublicGalleryDTO> findGallery(String code, LongFunction<String> uploadedImageUrl) {
        return findGalleries(List.of(code), uploadedImageUrl).stream().findFirst();
    }

    /** Ảnh upload theo id (kèm dữ liệu nhị phân và content type), để phục vụ file ảnh. */
    public Optional<GalleryImage> findUploadedImage(Long id) {
        return repository.findUploadedImage(id);
    }

    private static PublicGalleryDTO.Image toImage(GalleryPublicRow row, LongFunction<String> uploadedImageUrl) {
        // Ưu tiên ảnh upload; không có thì dùng link ảnh có sẵn
        String url = row.uploaded() ? uploadedImageUrl.apply(row.id()) : row.imageUrl();
        return new PublicGalleryDTO.Image(
            row.id(),
            row.title(),
            url,
            row.linkUrl(),
            row.altText(),
            Boolean.TRUE.equals(row.openInNewTab()),
            row.displayOrder() == null ? 0 : row.displayOrder()
        );
    }
}
