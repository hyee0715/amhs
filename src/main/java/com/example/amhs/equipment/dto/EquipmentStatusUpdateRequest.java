package com.example.amhs.equipment.dto;

import com.example.amhs.equipment.domain.EquipmentStatus;
import jakarta.validation.constraints.NotNull;

public record EquipmentStatusUpdateRequest(
        @NotNull(message = "status is required")
        EquipmentStatus status
) {
}
