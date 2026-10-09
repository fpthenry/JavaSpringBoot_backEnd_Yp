package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.service.PublicLocationService;
import com.mycompany.myapp.service.dto.publicapi.PublicLocationDTO;
import com.mycompany.myapp.service.dto.publicapi.PublicPageDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mục lục tỉnh thành theo chữ cái cho trang quản trị (đăng nhập JWT). Cùng dữ liệu và cách lọc với API công khai
 * {@code /api/public/v1/locations} ({@link PublicLocationService}), nhưng không cần khóa API của FE.
 */
@RestController
@RequestMapping("/api/location-index")
@Tag(name = "location-index", description = "Mục lục tỉnh thành theo chữ cái, kèm số doanh nghiệp (trang quản trị)")
public class LocationIndexResource {

    private final PublicLocationService locationService;

    public LocationIndexResource(PublicLocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/letters")
    @Operation(
        summary = "Các chữ cái của mục lục tỉnh thành",
        description = "A–D, Đ, E–Z rồi '#', kèm số đơn vị. Chữ cái theo tên riêng (bỏ \"Xã\", \"Phường\"...)."
    )
    public List<PublicLocationDTO.Letter> getLetters(
        @Parameter(description = "new (mặc định), old hoặc all") @RequestParam(name = "generation", required = false) String generation,
        @Parameter(description = "province, district hoặc ward") @RequestParam(name = "level", required = false) String level,
        @Parameter(description = "true: chỉ đếm đơn vị đã có doanh nghiệp") @RequestParam(
            name = "hideEmpty",
            defaultValue = "false"
        ) boolean hideEmpty
    ) {
        return locationService.letters(generation, level, hideEmpty);
    }

    @GetMapping
    @Operation(
        summary = "Tỉnh thành theo chữ cái đầu và/hoặc tên, kèm số doanh nghiệp",
        description = "letter: một chữ cái, Đ hoặc '#'; q: từ khóa khớp đầu từ trong tên đơn vị hoặc tỉnh, không phân biệt dấu; sort: " +
            "name (mặc định) hoặc listingCount; size tối đa 500."
    )
    public PublicPageDTO<PublicLocationDTO> getLocations(
        @Parameter(description = "Chữ cái đầu, ví dụ C") @RequestParam(name = "letter", required = false) String letter,
        @Parameter(description = "Từ khóa trong tên") @RequestParam(name = "q", required = false) String q,
        @Parameter(description = "new (mặc định), old hoặc all") @RequestParam(name = "generation", required = false) String generation,
        @Parameter(description = "province, district hoặc ward") @RequestParam(name = "level", required = false) String level,
        @Parameter(description = "true: bỏ đơn vị chưa có doanh nghiệp") @RequestParam(
            name = "hideEmpty",
            defaultValue = "false"
        ) boolean hideEmpty,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        return locationService.locations(letter, q, generation, level, hideEmpty, pageable);
    }
}
