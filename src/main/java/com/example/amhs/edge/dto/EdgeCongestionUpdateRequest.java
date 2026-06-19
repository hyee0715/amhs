package com.example.amhs.edge.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record EdgeCongestionUpdateRequest(
        @Min(value = 0, message = "congestionLevel must be greater than or equal to 0")
        @Max(value = 100, message = "congestionLevel must be less than or equal to 100")
        int congestionLevel
) {
}
