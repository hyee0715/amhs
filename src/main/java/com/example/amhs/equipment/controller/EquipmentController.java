package com.example.amhs.equipment.controller;

import com.example.amhs.equipment.dto.EquipmentCreateRequest;
import com.example.amhs.equipment.dto.EquipmentResponse;
import com.example.amhs.equipment.dto.EquipmentStatusUpdateRequest;
import com.example.amhs.equipment.service.EquipmentService;
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
public class EquipmentController {

    private final EquipmentService equipmentService;

    @PostMapping
    public ResponseEntity<EquipmentResponse> createEquipment(
            @Valid @RequestBody EquipmentCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(equipmentService.createEquipment(request));
    }

    @GetMapping
    public ResponseEntity<List<EquipmentResponse>> getEquipments() {
        return ResponseEntity.ok(equipmentService.getEquipments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipmentResponse> getEquipment(@PathVariable Long id) {
        return ResponseEntity.ok(equipmentService.getEquipment(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<EquipmentResponse> updateEquipmentStatus(
            @PathVariable Long id,
            @Valid @RequestBody EquipmentStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(equipmentService.updateEquipmentStatus(id, request));
    }
}
