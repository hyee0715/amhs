package com.example.amhs.edge.controller;

import com.example.amhs.edge.dto.EdgeCongestionUpdateRequest;
import com.example.amhs.edge.dto.EdgeCreateRequest;
import com.example.amhs.edge.dto.EdgeResponse;
import com.example.amhs.edge.dto.EdgeStatusUpdateRequest;
import com.example.amhs.edge.service.EdgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/edges")
@Tag(name = "Edge", description = "AMHS Edge 관리 API")
public class EdgeController {

    private final EdgeService edgeService;

    @PostMapping
    @Operation(summary = "Edge 등록")
    public ResponseEntity<List<EdgeResponse>> createEdge(@Valid @RequestBody EdgeCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(edgeService.createEdge(request));
    }

    @GetMapping
    @Operation(summary = "Edge 목록 조회")
    public ResponseEntity<List<EdgeResponse>> getEdges() {
        return ResponseEntity.ok(edgeService.getEdges());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Edge 단건 조회")
    public ResponseEntity<EdgeResponse> getEdge(@PathVariable Long id) {
        return ResponseEntity.ok(edgeService.getEdge(id));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Edge 상태 변경")
    public ResponseEntity<EdgeResponse> updateEdgeStatus(
            @PathVariable Long id,
            @Valid @RequestBody EdgeStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(edgeService.updateEdgeStatus(id, request));
    }

    @PatchMapping("/{id}/congestion")
    @Operation(summary = "Edge 혼잡도 변경")
    public ResponseEntity<EdgeResponse> updateEdgeCongestion(
            @PathVariable Long id,
            @Valid @RequestBody EdgeCongestionUpdateRequest request
    ) {
        return ResponseEntity.ok(edgeService.updateEdgeCongestion(id, request));
    }
}
