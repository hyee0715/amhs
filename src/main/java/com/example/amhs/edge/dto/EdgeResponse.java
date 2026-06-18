package com.example.amhs.edge.dto;

import com.example.amhs.edge.domain.AmhsEdge;
import com.example.amhs.edge.domain.EdgeStatus;
import java.time.LocalDateTime;

public record EdgeResponse(
        Long id,
        String fromNodeCode,
        String toNodeCode,
        int distance,
        int estimatedTimeSeconds,
        int congestionLevel,
        EdgeStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static EdgeResponse from(AmhsEdge edge) {
        return new EdgeResponse(
                edge.getId(),
                edge.getFromNode().getCode(),
                edge.getToNode().getCode(),
                edge.getDistance(),
                edge.getEstimatedTimeSeconds(),
                edge.getCongestionLevel(),
                edge.getStatus(),
                edge.getCreatedAt(),
                edge.getUpdatedAt()
        );
    }
}
