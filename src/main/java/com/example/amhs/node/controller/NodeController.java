package com.example.amhs.node.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.amhs.node.dto.NodeCreateRequest;
import com.example.amhs.node.dto.NodeResponse;
import com.example.amhs.node.dto.NodeStatusUpdateRequest;
import com.example.amhs.node.service.NodeService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/nodes")
@Tag(name = "Node", description = "AMHS Node 관리 API")
public class NodeController {

    private final NodeService nodeService;

    @PostMapping
    @Operation(summary = "Node 등록")
    public ResponseEntity<NodeResponse> createNode(@Valid @RequestBody NodeCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(nodeService.createNode(request));
    }

    @GetMapping
    @Operation(summary = "Node 목록 조회")
    public ResponseEntity<List<NodeResponse>> getNodes() {
        return ResponseEntity.ok(nodeService.getNodes());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Node 단건 조회")
    public ResponseEntity<NodeResponse> getNode(@PathVariable Long id) {
        return ResponseEntity.ok(nodeService.getNode(id));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Node 상태 변경")
    public ResponseEntity<NodeResponse> updateNodeStatus(
            @PathVariable Long id,
            @Valid @RequestBody NodeStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(nodeService.updateNodeStatus(id, request));
    }
}
