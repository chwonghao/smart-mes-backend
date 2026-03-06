package com.smartmes.backend.modules.masterdata.controller;

import com.smartmes.backend.modules.masterdata.dto.ItemMasterDto;
import com.smartmes.backend.modules.masterdata.service.ItemMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/master-data/items")
@RequiredArgsConstructor
@Tag(name = "1. Master Data - Quản lý Vật tư", description = "Các API dùng để thêm, sửa, xóa danh mục nguyên vật liệu và thành phẩm")
public class ItemMasterController {

    private final ItemMasterService service;
    private final String CURRENT_TENANT_ID = "TENANT_01"; // Tạm thời hardcode để test

    @PostMapping
    @Operation(summary = "Tạo mới một vật tư/sản phẩm", description = "Lưu dữ liệu vật tư mới vào hệ thống kèm theo các thuộc tính động (JSONB).")
    public ResponseEntity<?> createItem(@RequestBody ItemMasterDto dto) {
        try {
            return ResponseEntity.ok(service.createItem(dto, CURRENT_TENANT_ID));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách vật tư", description = "Trả về toàn bộ danh sách vật tư của doanh nghiệp đang đăng nhập.")
    public ResponseEntity<?> getAllItems() {
        return ResponseEntity.ok(service.getAllItems(CURRENT_TENANT_ID));
    }
}