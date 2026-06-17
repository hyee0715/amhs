package com.example.amhs.edge.dto;

import com.example.amhs.edge.domain.EdgeStatus;
import jakarta.validation.constraints.NotNull;

public record EdgeStatusUpdateRequest(
        @NotNull(message = "status is required")
        EdgeStatus status
) {
}
