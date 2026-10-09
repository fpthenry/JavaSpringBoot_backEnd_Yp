package com.mycompany.myapp.web.rest.publicapi;

import com.mycompany.myapp.service.PublicLocationService;
import com.mycompany.myapp.service.dto.publicapi.PublicLocationDTO;
import com.mycompany.myapp.service.dto.publicapi.PublicPageDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API công khai tỉnh thành cho FE (Next.js): mục lục theo chữ cái. Cần header {@code X-API-Key}.
 */
@RestController
@RequestMapping("/api/public/v1/locations")
@Tag(name = "public-location", description = "API công khai: tỉnh thành, mục lục theo chữ cái (cần header X-API-Key)")
public class PublicLocationResource {

    /** Địa phương ít thay đổi: FE cache 5 phút. */
    private static final CacheControl JSON_CACHE = CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic();

    private final PublicLocationService locationService;

    public PublicLocationResource(PublicLocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/letters")
    @Operation(
        summary = "Các chữ cái của mục lục tỉnh thành",
        description = "Mỗi chữ cái có đơn vị hành chính, kèm số đơn vị: A–D, Đ, E–Z, '#' cuối. Chữ cái lấy theo tên riêng " +
            "(\"Xã Châu Thới\" → C); bỏ dấu (Ô → O) nhưng Đ là chữ riêng. Dùng cho dãy nút chữ cái."
    )
    public ResponseEntity<List<PublicLocationDTO.Letter>> getLetters(
        @Parameter(description = "new (mặc định, địa giới từ 01/7/2025), old hoặc all") @RequestParam(
            name = "generation",
            required = false
        ) String generation,
        @Parameter(description = "province, district (chỉ bộ cũ) hoặc ward; bỏ trống = mọi cấp") @RequestParam(
            name = "level",
            required = false
        ) String level,
        @Parameter(description = "true: chỉ đếm đơn vị đã có doanh nghiệp") @RequestParam(
            name = "hideEmpty",
            defaultValue = "false"
        ) boolean hideEmpty
    ) {
        return ResponseEntity.ok()
            .cacheControl(JSON_CACHE)
            .body(locationService.letters(generation, level, hideEmpty));
    }

    @GetMapping
    @Operation(
        summary = "Tỉnh thành theo chữ cái đầu và/hoặc từ khóa, kèm số doanh nghiệp",
        description = "letter: một chữ cái (l = L; Đ khác D) hoặc '#'; bỏ trống = tất cả. q: từ khóa (khớp đầu từ, \"dong anh\" → Đông Anh) trong tên đơn vị hoặc " +
            "tên tỉnh, không phân biệt dấu. listingCount: số doanh nghiệp đã xuất bản, tính cả đơn vị con. provinceName " +
            "để phân biệt xã trùng tên. Mặc định sắp theo tên riêng (A–Z tiếng Việt); sort=listingCount,desc để nơi nhiều " +
            "doanh nghiệp lên trước. size tối đa 500."
    )
    public ResponseEntity<PublicPageDTO<PublicLocationDTO>> getLocations(
        @Parameter(description = "Chữ cái đầu, ví dụ C") @RequestParam(name = "letter", required = false) String letter,
        @Parameter(description = "Từ khóa, ví dụ chau thoi") @RequestParam(name = "q", required = false) String q,
        @Parameter(description = "new (mặc định), old hoặc all") @RequestParam(name = "generation", required = false) String generation,
        @Parameter(description = "province, district hoặc ward") @RequestParam(name = "level", required = false) String level,
        @Parameter(description = "true: bỏ đơn vị chưa có doanh nghiệp") @RequestParam(
            name = "hideEmpty",
            defaultValue = "false"
        ) boolean hideEmpty,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok()
            .cacheControl(JSON_CACHE)
            .body(locationService.locations(letter, q, generation, level, hideEmpty, pageable));
    }
}
