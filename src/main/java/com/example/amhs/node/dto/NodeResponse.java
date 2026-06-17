package com.example.amhs.node.dto;

import com.example.amhs.node.domain.AmhsNode;
import com.example.amhs.node.domain.NodeStatus;
import com.example.amhs.node.domain.NodeType;
import java.time.LocalDateTime;

public record NodeResponse(
        Long id,
        String code,
        String name,
        NodeType type,
        NodeStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static NodeResponse from(AmhsNode node) {
        return new NodeResponse(
                node.getId(),
                node.getCode(),
                node.getName(),
                node.getType(),
                node.getStatus(),
                node.getCreatedAt(),
                node.getUpdatedAt()
        );
    }
}
