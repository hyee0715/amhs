package com.example.amhs.edge.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;

public record EdgeCreateRequest(
        @NotBlank(message = "fromNodeCode is required")
        String fromNodeCode,

        @NotBlank(message = "toNodeCode is required")
        String toNodeCode,

        @Min(value = 1, message = "distance must be greater than 0")
        int distance,

        @Min(value = 1, message = "estimatedTimeSeconds must be greater than 0")
        int estimatedTimeSeconds,

        @Min(value = 0, message = "congestionLevel must be greater than or equal to 0")
        @Max(value = 100, message = "congestionLevel must be less than or equal to 100")
        int congestionLevel,

        boolean bidirectional
) {
}
