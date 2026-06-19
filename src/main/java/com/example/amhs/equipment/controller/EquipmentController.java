package com.example.amhs.equipment.controller;

import com.example.amhs.equipment.dto.EquipmentCreateRequest;
import com.example.amhs.equipment.dto.EquipmentResponse;
import com.example.amhs.equipment.dto.EquipmentStatusUpdateRequest;
import com.example.amhs.equipment.service.EquipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/equipments")
@Tag(name = "Equipment", description = "장비 관리 API")
public class EquipmentController {

    private final EquipmentService equipmentService;

    @PostMapping
    @Operation(summary = "장비 등록")
    public ResponseEntity<EquipmentResponse> createEquipment(
            @Valid @RequestBody EquipmentCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(equipmentService.createEquipment(request));
    }

    @GetMapping
    @Operation(summary = "장비 목록 조회")
    public ResponseEntity<List<EquipmentResponse>> getEquipments() {
        return ResponseEntity.ok(equipmentService.getEquipments());
    }

    @GetMapping("/{id}")
    @Operation(summary = "장비 단건 조회")
    public ResponseEntity<EquipmentResponse> getEquipment(@PathVariable Long id) {
        return ResponseEntity.ok(equipmentService.getEquipment(id));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "장비 상태 변경")
    public ResponseEntity<EquipmentResponse> updateEquipmentStatus(
            @PathVariable Long id,
            @Valid @RequestBody EquipmentStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(equipmentService.updateEquipmentStatus(id, request));
    }
}
