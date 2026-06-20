package com.example.amhs.analytics.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TransferTimeOutlierJobResponse(
        Long jobId,
        String carrierId,
        String sourceNodeCode,
        String destinationNodeCode,
        Integer actualTransferTimeSeconds,
        List<String> path,
        LocalDateTime completedAt
) {
}
