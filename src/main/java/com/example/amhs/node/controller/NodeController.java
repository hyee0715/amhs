package com.example.amhs.node.controller;

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
public class NodeController {

    private final NodeService nodeService;

    @PostMapping
    public ResponseEntity<NodeResponse> createNode(@Valid @RequestBody NodeCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(nodeService.createNode(request));
    }

    @GetMapping
    public ResponseEntity<List<NodeResponse>> getNodes() {
        return ResponseEntity.ok(nodeService.getNodes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NodeResponse> getNode(@PathVariable Long id) {
        return ResponseEntity.ok(nodeService.getNode(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<NodeResponse> updateNodeStatus(
            @PathVariable Long id,
            @Valid @RequestBody NodeStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(nodeService.updateNodeStatus(id, request));
    }
}
