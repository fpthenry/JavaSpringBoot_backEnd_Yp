package com.mycompany.myapp.web.rest.publicapi;

import com.mycompany.myapp.service.PublicGalleryService;
import com.mycompany.myapp.service.dto.publicapi.PublicGalleryDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.function.LongFunction;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * API công khai gallery/banner quảng cáo cho FE (Next.js). Cần header {@code X-API-Key}, trừ endpoint ảnh.
 */
@RestController
@RequestMapping("/api/public/v1")
@Tag(name = "public-gallery", description = "API công khai: banner quảng cáo (cần header X-API-Key, trừ file ảnh)")
public class PublicGalleryResource {

    static final String MEDIA_PATH = "/api/public/v1/media/gallery-images/{id}";

    /** FE (Next.js) có thể cache JSON trong thời gian này; quản trị viên đổi banner thì tối đa sau chừng ấy mới thấy. */
    private static final CacheControl JSON_CACHE = CacheControl.maxAge(Duration.ofMinutes(1)).cachePublic();
    private static final CacheControl IMAGE_CACHE = CacheControl.maxAge(Duration.ofHours(1)).cachePublic();

    private final PublicGalleryService galleryService;

    public PublicGalleryResource(PublicGalleryService galleryService) {
        this.galleryService = galleryService;
    }

    @GetMapping("/galleries")
    @Operation(
        summary = "Các vị trí banner đang bật, kèm ảnh đang chạy",
        description = "Mỗi vị trí trả ảnh active, trong khoảng startAt-endAt, theo displayOrder. " +
            "Lọc theo mã: ?codes=footer-banner,right-banner-1. Bỏ trống = tất cả vị trí đang bật."
    )
    public ResponseEntity<List<PublicGalleryDTO>> getGalleries(
        @Parameter(description = "Mã vị trí, cách nhau bởi dấu phẩy") @RequestParam(name = "codes", required = false) List<String> codes
    ) {
        return ResponseEntity.ok().cacheControl(JSON_CACHE).body(galleryService.findGalleries(codes, uploadedImageUrl()));
    }

    @GetMapping("/galleries/{code}")
    @Operation(summary = "Một vị trí banner theo mã, kèm ảnh đang chạy", description = "404 nếu không có hoặc đang tắt.")
    public ResponseEntity<PublicGalleryDTO> getGallery(@PathVariable("code") String code) {
        return galleryService
            .findGallery(code, uploadedImageUrl())
            .map(gallery -> ResponseEntity.ok().cacheControl(JSON_CACHE).body(gallery))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/media/gallery-images/{id}")
    @Operation(
        summary = "File ảnh banner đã upload",
        description = "Không cần X-API-Key, để thẻ img trên trình duyệt tải trực tiếp. Có ETag và Cache-Control 1 giờ."
    )
    public ResponseEntity<byte[]> getUploadedImage(@PathVariable("id") Long id, WebRequest request) {
        return galleryService
            .findUploadedImage(id)
            .map(image -> {
                String etag = etag(image.getImage());
                if (request.checkNotModified(etag)) {
                    return ResponseEntity.status(304).eTag(etag).cacheControl(IMAGE_CACHE).<byte[]>build();
                }
                MediaType type =
                    image.getImageContentType() == null
                        ? MediaType.APPLICATION_OCTET_STREAM
                        : MediaType.parseMediaType(image.getImageContentType());
                return ResponseEntity.ok().contentType(type).eTag(etag).cacheControl(IMAGE_CACHE).body(image.getImage());
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** URL tuyệt đối tới endpoint ảnh, theo địa chỉ server mà FE đang gọi. */
    private static LongFunction<String> uploadedImageUrl() {
        String base = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        return id -> base + MEDIA_PATH.replace("{id}", String.valueOf(id));
    }

    private static String etag(byte[] bytes) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes);
            return "\"" + HexFormat.of().formatHex(hash, 0, 16) + "\"";
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 luôn có trong JDK
            throw new IllegalStateException(e);
        }
    }
}
