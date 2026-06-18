package com.example.amhs.dashboard.service;

import com.example.amhs.dashboard.dto.DashboardSummaryResponse;
import com.example.amhs.edge.domain.EdgeStatus;
import com.example.amhs.edge.repository.EdgeRepository;
import com.example.amhs.equipment.domain.EquipmentStatus;
import com.example.amhs.equipment.repository.EquipmentRepository;
import com.example.amhs.node.domain.NodeStatus;
import com.example.amhs.node.repository.NodeRepository;
import com.example.amhs.transferjob.domain.TransferJobStatus;
import com.example.amhs.transferjob.repository.TransferJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final TransferJobRepository transferJobRepository;
    private final NodeRepository nodeRepository;
    private final EdgeRepository edgeRepository;
    private final EquipmentRepository equipmentRepository;

    public DashboardSummaryResponse getSummary() {
        long totalJobs = transferJobRepository.count();
        long createdJobs = transferJobRepository.countByStatus(TransferJobStatus.CREATED);
        long movingJobs = transferJobRepository.countByStatus(TransferJobStatus.MOVING);
        long completedJobs = transferJobRepository.countByStatus(TransferJobStatus.COMPLETED);
        long failedJobs = transferJobRepository.countByStatus(TransferJobStatus.FAILED);

        double successRate = totalJobs == 0 ? 0.0 : (completedJobs * 100.0) / totalJobs;
        double averageEstimatedTimeSeconds = transferJobRepository.findAverageEstimatedTimeSeconds();

        return new DashboardSummaryResponse(
                totalJobs,
                createdJobs,
                movingJobs,
                completedJobs,
                failedJobs,
                successRate,
                averageEstimatedTimeSeconds,
                nodeRepository.countByStatus(NodeStatus.BLOCKED),
                edgeRepository.countByStatus(EdgeStatus.BLOCKED),
                equipmentRepository.countByStatus(EquipmentStatus.ERROR)
        );
    }
}
