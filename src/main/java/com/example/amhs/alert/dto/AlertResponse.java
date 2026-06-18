package com.example.amhs.alert.dto;

import com.example.amhs.alert.domain.Alert;
import com.example.amhs.alert.domain.AlertStatus;
import com.example.amhs.alert.domain.AlertType;
import java.time.LocalDateTime;

public record AlertResponse(
        Long id,
        Long transferJobId,
        String assignedEquipmentCode,
        AlertType type,
        AlertStatus status,
        String message,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime resolvedAt
) {
    public static AlertResponse from(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getTransferJob() != null ? alert.getTransferJob().getId() : null,
                alert.getTransferJob() != null && alert.getTransferJob().getAssignedEquipment() != null
                        ? alert.getTransferJob().getAssignedEquipment().getCode()
                        : null,
                alert.getType(),
                alert.getStatus(),
                alert.getMessage(),
                alert.getCreatedAt(),
                alert.getUpdatedAt(),
                alert.getResolvedAt()
        );
    }
}
