package com.example.amhs.transferjob.dto;

import com.example.amhs.transferjob.domain.TransferJobStatus;
import jakarta.validation.constraints.NotNull;

public record TransferJobStatusUpdateRequest(
        @NotNull(message = "status is required")
        TransferJobStatus status,
        String reason,
        String assignedEquipmentCode
) {
}
