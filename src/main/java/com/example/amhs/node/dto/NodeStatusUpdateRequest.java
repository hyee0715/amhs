package com.example.amhs.node.dto;

import com.example.amhs.node.domain.NodeStatus;
import jakarta.validation.constraints.NotNull;

public record NodeStatusUpdateRequest(
        @NotNull(message = "status is required")
        NodeStatus status
) {
}
