package com.example.amhs.transferjob.dto;

import com.example.amhs.transferjob.domain.TransferJobHistory;
import com.example.amhs.transferjob.domain.TransferJobStatus;
import java.time.LocalDateTime;
import java.util.List;

public record TransferJobHistoryResponse(
        Long id,
        TransferJobStatus status,
        String reason,
        String assignedEquipmentCode,
        List<String> pathSnapshot,
        LocalDateTime createdAt
) {
    public static TransferJobHistoryResponse from(TransferJobHistory history, List<String> pathSnapshot) {
        return new TransferJobHistoryResponse(
                history.getId(),
                history.getStatus(),
                history.getReason(),
                history.getAssignedEquipmentCode(),
                pathSnapshot,
                history.getCreatedAt()
        );
    }
}
