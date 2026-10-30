package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.service.PublicLocationService;
import com.mycompany.myapp.service.dto.publicapi.PublicLocationDTO;
import com.mycompany.myapp.service.dto.publicapi.PublicPageDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.jhipster.web.util.ResponseUtil;

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

    @GetMapping("/all")
    @Operation(
        summary = "Mọi đơn vị hành chính của một bộ địa giới, cho cây tra cứu",
        description = "generation: new (mặc định, từ 01/7/2025: tỉnh → phường, xã, đặc khu) hoặc old (trước 01/7/2025: tỉnh → " +
            "quận, huyện → xã). Mỗi nút: id, mã ĐVHC, tên, cấp, id cha, số doanh nghiệp, mapped (có trong bảng chuyển đổi)."
    )
    public List<PublicLocationDTO.Node> getAllNodes(
        @Parameter(description = "new hoặc old") @RequestParam(name = "generation", required = false) String generation
    ) {
        return locationService.nodes(generation);
    }

    @GetMapping("/origins/{slugOrId}")
    @Operation(summary = "Đơn vị mới hình thành từ các đơn vị cũ nào (kèm id đơn vị cũ, nhập một phần hay toàn bộ)")
    public ResponseEntity<List<PublicLocationDTO.Origin>> getOrigins(
        @Parameter(description = "Slug hoặc id đơn vị mới") @PathVariable("slugOrId") String slugOrId
    ) {
        return ResponseUtil.wrapOrNotFound(locationService.origins(slugOrId));
    }

    @GetMapping("/successors/{slugOrId}")
    @Operation(summary = "Đơn vị cũ nay thuộc đơn vị mới nào (xã cũ → xã mới, huyện cũ → các xã mới, tỉnh cũ → tỉnh mới)")
    public ResponseEntity<List<PublicLocationDTO>> getSuccessors(
        @Parameter(description = "Slug hoặc id đơn vị cũ") @PathVariable("slugOrId") String slugOrId
    ) {
        return ResponseUtil.wrapOrNotFound(locationService.successors(slugOrId));
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
