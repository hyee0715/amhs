package com.example.amhs.transferjob.controller;

import com.example.amhs.transferjob.dto.TransferJobCreateRequest;
import com.example.amhs.transferjob.dto.TransferJobHistoryResponse;
import com.example.amhs.transferjob.dto.TransferJobResponse;
import com.example.amhs.transferjob.dto.TransferJobStatusUpdateRequest;
import com.example.amhs.transferjob.service.TransferJobService;
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
public class TransferJobController {

    private final TransferJobService transferJobService;

    @PostMapping
    public ResponseEntity<TransferJobResponse> createTransferJob(
            @Valid @RequestBody TransferJobCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transferJobService.createTransferJob(request));
    }

    @GetMapping
    public ResponseEntity<List<TransferJobResponse>> getTransferJobs() {
        return ResponseEntity.ok(transferJobService.getTransferJobs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransferJobResponse> getTransferJob(@PathVariable Long id) {
        return ResponseEntity.ok(transferJobService.getTransferJob(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TransferJobResponse> updateTransferJobStatus(
            @PathVariable Long id,
            @Valid @RequestBody TransferJobStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(transferJobService.updateTransferJobStatus(id, request));
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<TransferJobResponse> retryTransferJob(@PathVariable Long id) {
        return ResponseEntity.ok(transferJobService.retryTransferJob(id));
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<TransferJobResponse> assignTransferJob(@PathVariable Long id) {
        return ResponseEntity.ok(transferJobService.assignTransferJob(id));
    }

    @PostMapping("/assign-pending")
    public ResponseEntity<List<TransferJobResponse>> assignPendingTransferJobs() {
        return ResponseEntity.ok(transferJobService.assignPendingTransferJobs());
    }

    @GetMapping("/{id}/histories")
    public ResponseEntity<List<TransferJobHistoryResponse>> getTransferJobHistories(@PathVariable Long id) {
        return ResponseEntity.ok(transferJobService.getTransferJobHistories(id));
    }
}
