package com.example.amhs.node.dto;

import com.example.amhs.node.domain.NodeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NodeCreateRequest(
        @NotBlank(message = "code is required")
        @Size(max = 100, message = "code must be less than or equal to 100 characters")
        String code,

        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be less than or equal to 100 characters")
        String name,

        @NotNull(message = "type is required")
        NodeType type
) {
}
