package com.example.amhs.transferjob.dto;

import com.example.amhs.transferjob.domain.TransferJob;
import com.example.amhs.transferjob.domain.TransferJobPriority;
import com.example.amhs.transferjob.domain.TransferJobStatus;
import java.time.LocalDateTime;
import java.util.List;

public record TransferJobResponse(
        Long id,
        String carrierId,
        String sourceNodeCode,
        String destinationNodeCode,
        String assignedEquipmentCode,
        TransferJobStatus status,
        TransferJobPriority priority,
        List<String> path,
        int estimatedTimeSeconds,
        int retryCount,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime failedAt,
        Integer actualTransferTimeSeconds
) {
    public static TransferJobResponse from(TransferJob transferJob, List<String> path) {
        return new TransferJobResponse(
                transferJob.getId(),
                transferJob.getCarrierId(),
                transferJob.getSourceNode().getCode(),
                transferJob.getDestinationNode().getCode(),
                transferJob.getAssignedEquipment() != null ? transferJob.getAssignedEquipment().getCode() : null,
                transferJob.getStatus(),
                transferJob.getPriority(),
                path,
                transferJob.getEstimatedTimeSeconds(),
                transferJob.getRetryCount(),
                transferJob.getFailureReason(),
                transferJob.getCreatedAt(),
                transferJob.getUpdatedAt(),
                transferJob.getStartedAt(),
                transferJob.getCompletedAt(),
                transferJob.getFailedAt(),
                transferJob.getActualTransferTimeSeconds()
        );
    }
}
