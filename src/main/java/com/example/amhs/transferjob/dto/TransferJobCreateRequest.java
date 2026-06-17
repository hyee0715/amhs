package com.example.amhs.transferjob.dto;

import com.example.amhs.transferjob.domain.TransferJobPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TransferJobCreateRequest(
        @NotBlank(message = "carrierId is required")
        String carrierId,

        @NotBlank(message = "sourceNodeCode is required")
        String sourceNodeCode,

        @NotBlank(message = "destinationNodeCode is required")
        String destinationNodeCode,

        @NotNull(message = "priority is required")
        TransferJobPriority priority
) {
}
