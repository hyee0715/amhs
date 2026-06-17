package com.example.amhs.edge.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record EdgeCreateRequest(
        @NotBlank(message = "fromNodeCode is required")
        String fromNodeCode,

        @NotBlank(message = "toNodeCode is required")
        String toNodeCode,

        @Min(value = 1, message = "distance must be greater than 0")
        int distance,

        @Min(value = 1, message = "estimatedTimeSeconds must be greater than 0")
        int estimatedTimeSeconds,

        boolean bidirectional
) {
}
