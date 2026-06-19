package com.example.amhs.transferjob.controller;

import com.example.amhs.transferjob.dto.TransferJobCreateRequest;
import com.example.amhs.transferjob.dto.TransferJobHistoryResponse;
import com.example.amhs.transferjob.dto.TransferJobResponse;
import com.example.amhs.transferjob.dto.TransferJobStatusUpdateRequest;
import com.example.amhs.transferjob.service.TransferJobService;
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
@RequestMapping("/api/transfer-jobs")
@Tag(name = "Transfer Job", description = "반송 Job 관리 API")
public class TransferJobController {

    private final TransferJobService transferJobService;

    @PostMapping
    @Operation(summary = "Transfer Job 생성")
    public ResponseEntity<TransferJobResponse> createTransferJob(
            @Valid @RequestBody TransferJobCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transferJobService.createTransferJob(request));
    }

    @GetMapping
    @Operation(summary = "Transfer Job 목록 조회")
    public ResponseEntity<List<TransferJobResponse>> getTransferJobs() {
        return ResponseEntity.ok(transferJobService.getTransferJobs());
    }

    @GetMapping("/dispatch-candidates")
    @Operation(summary = "배정 후보 Queue 조회")
    public ResponseEntity<List<TransferJobResponse>> getDispatchCandidates() {
        return ResponseEntity.ok(transferJobService.getDispatchCandidates());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Transfer Job 단건 조회")
    public ResponseEntity<TransferJobResponse> getTransferJob(@PathVariable Long id) {
        return ResponseEntity.ok(transferJobService.getTransferJob(id));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Transfer Job 상태 변경")
    public ResponseEntity<TransferJobResponse> updateTransferJobStatus(
            @PathVariable Long id,
            @Valid @RequestBody TransferJobStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(transferJobService.updateTransferJobStatus(id, request));
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "실패 Job 재처리")
    public ResponseEntity<TransferJobResponse> retryTransferJob(@PathVariable Long id) {
        return ResponseEntity.ok(transferJobService.retryTransferJob(id));
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "개별 Job 장비 자동 할당")
    public ResponseEntity<TransferJobResponse> assignTransferJob(@PathVariable Long id) {
        return ResponseEntity.ok(transferJobService.assignTransferJob(id));
    }

    @PostMapping("/assign-pending")
    @Operation(summary = "대기 Job 일괄 자동 할당")
    public ResponseEntity<List<TransferJobResponse>> assignPendingTransferJobs() {
        return ResponseEntity.ok(transferJobService.assignPendingTransferJobs());
    }

    @GetMapping("/{id}/histories")
    @Operation(summary = "Transfer Job 이력 조회")
    public ResponseEntity<List<TransferJobHistoryResponse>> getTransferJobHistories(@PathVariable Long id) {
        return ResponseEntity.ok(transferJobService.getTransferJobHistories(id));
    }
}
