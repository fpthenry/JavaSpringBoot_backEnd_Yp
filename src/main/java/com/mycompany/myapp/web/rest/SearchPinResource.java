package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.service.SearchPinService;
import com.mycompany.myapp.service.SearchPinService.NewPin;
import com.mycompany.myapp.service.SearchPinService.PinGroup;
import com.mycompany.myapp.service.SearchPinService.Reorder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Viết tay (2026-11-17): ghim doanh nghiệp lên đầu kết quả tìm kiếm theo từ khóa.
 * Quyền ở SecurityConfiguration: xem cho người xem doanh nghiệp, sửa cho ROLE_ADMIN và ROLE_LISTING_EDIT.
 */
@RestController
@RequestMapping("/api/search-pins")
@Tag(name = "search-pins", description = "Ghim doanh nghiệp lên đầu kết quả tìm kiếm theo từ khóa")
public class SearchPinResource {

    private final SearchPinService service;

    public SearchPinResource(SearchPinService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Mọi từ khóa có ghim, kèm doanh nghiệp theo thứ tự")
    public List<PinGroup> groups() {
        return service.groups();
    }

    @PostMapping
    @Operation(summary = "Ghim một doanh nghiệp cho từ khóa (xếp cuối danh sách ghim của từ khóa)")
    public PinGroup add(@RequestBody NewPin request) {
        return service.addPin(request);
    }

    @PutMapping("/order")
    @Operation(summary = "Xếp lại thứ tự các doanh nghiệp ghim của một từ khóa")
    public PinGroup reorder(@RequestBody Reorder request) {
        return service.reorder(request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Bỏ ghim một doanh nghiệp")
    public ResponseEntity<Void> remove(@PathVariable("id") Long id) {
        service.removePin(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Xóa cả từ khóa (bỏ ghim mọi doanh nghiệp của nó)")
    public ResponseEntity<Void> removeGroup(@RequestParam("keyword") String keyword) {
        service.removeGroup(keyword);
        return ResponseEntity.noContent().build();
    }
}
