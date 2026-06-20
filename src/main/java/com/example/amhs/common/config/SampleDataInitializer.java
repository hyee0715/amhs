package com.example.amhs.common.config;

import com.example.amhs.edge.domain.AmhsEdge;
import com.example.amhs.edge.repository.EdgeRepository;
import com.example.amhs.equipment.domain.Equipment;
import com.example.amhs.equipment.repository.EquipmentRepository;
import com.example.amhs.node.domain.AmhsNode;
import com.example.amhs.node.domain.NodeType;
import com.example.amhs.node.repository.NodeRepository;
import com.example.amhs.transferjob.domain.TransferJobPriority;
import com.example.amhs.transferjob.domain.TransferJobStatus;
import com.example.amhs.transferjob.dto.TransferJobCreateRequest;
import com.example.amhs.transferjob.dto.TransferJobStatusUpdateRequest;
import com.example.amhs.transferjob.repository.TransferJobRepository;
import com.example.amhs.transferjob.service.TransferJobService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local")
@RequiredArgsConstructor
public class SampleDataInitializer implements ApplicationRunner {

    private final NodeRepository nodeRepository;
    private final EdgeRepository edgeRepository;
    private final EquipmentRepository equipmentRepository;
    private final TransferJobService transferJobService;
    private final TransferJobRepository transferJobRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (hasAnyData()) {
            return;
        }

        initializeNodesAndEdges();
        initializeEquipments();
        initializeAnalyticsSampleJobs();
    }

    private boolean hasAnyData() {
        return nodeRepository.count() > 0 || edgeRepository.count() > 0 || equipmentRepository.count() > 0;
    }

    private void initializeNodesAndEdges() {
        AmhsNode stocker01 = nodeRepository.save(AmhsNode.create("STOCKER_01", "Stocker 01", NodeType.STOCKER));
        AmhsNode nodeA = nodeRepository.save(AmhsNode.create("NODE_A", "Node A", NodeType.OHT_NODE));
        AmhsNode nodeB = nodeRepository.save(AmhsNode.create("NODE_B", "Node B", NodeType.OHT_NODE));
        AmhsNode nodeC = nodeRepository.save(AmhsNode.create("NODE_C", "Node C", NodeType.OHT_NODE));
        AmhsNode nodeD = nodeRepository.save(AmhsNode.create("NODE_D", "Node D", NodeType.OHT_NODE));
        AmhsNode eqp01 = nodeRepository.save(AmhsNode.create("EQP_01", "Equipment 01", NodeType.EQP));
        AmhsNode eqp02 = nodeRepository.save(AmhsNode.create("EQP_02", "Equipment 02", NodeType.EQP));

        edgeRepository.saveAll(List.of(
                AmhsEdge.create(stocker01, nodeA, 100, 30),
                AmhsEdge.create(nodeA, nodeB, 120, 40),
                AmhsEdge.create(nodeB, eqp01, 80, 30),
                AmhsEdge.create(stocker01, nodeC, 150, 50),
                AmhsEdge.create(nodeC, nodeD, 70, 20),
                AmhsEdge.create(nodeD, eqp02, 90, 30),
                AmhsEdge.create(nodeA, nodeD, 85, 25),
                AmhsEdge.create(nodeA, stocker01, 100, 30),
                AmhsEdge.create(nodeB, nodeA, 120, 40),
                AmhsEdge.create(eqp01, nodeB, 80, 30),
                AmhsEdge.create(nodeC, stocker01, 150, 50),
                AmhsEdge.create(nodeD, nodeC, 70, 20),
                AmhsEdge.create(eqp02, nodeD, 90, 30),
                AmhsEdge.create(nodeD, nodeA, 85, 25)
        ));
    }

    private void initializeEquipments() {
        equipmentRepository.saveAll(List.of(
                Equipment.create("OHT_001", "OHT 001", com.example.amhs.equipment.domain.EquipmentType.OHT),
                Equipment.create("OHT_002", "OHT 002", com.example.amhs.equipment.domain.EquipmentType.OHT),
                Equipment.create("CONVEYOR_001", "Conveyor 001", com.example.amhs.equipment.domain.EquipmentType.CONVEYOR)
        ));
    }

    private void initializeAnalyticsSampleJobs() {
        initializeStableRouteCompletedJobs();
        initializeUnstableRouteCompletedJobs();
        initializeFailureAnalyticsJobs();
    }

    private void initializeStableRouteCompletedJobs() {
        createCompletedSampleJob("FOUP-STABLE-001", "EQP_01", 96, LocalDateTime.of(2026, 6, 18, 8, 10));
        createCompletedSampleJob("FOUP-STABLE-002", "EQP_01", 101, LocalDateTime.of(2026, 6, 18, 9, 5));
        createCompletedSampleJob("FOUP-STABLE-003", "EQP_01", 98, LocalDateTime.of(2026, 6, 18, 9, 35));
        createCompletedSampleJob("FOUP-STABLE-004", "EQP_01", 100, LocalDateTime.of(2026, 6, 18, 10, 10));
        createCompletedSampleJob("FOUP-STABLE-005", "EQP_01", 97, LocalDateTime.of(2026, 6, 18, 11, 0));
        createCompletedSampleJob("FOUP-STABLE-006", "EQP_01", 99, LocalDateTime.of(2026, 6, 18, 12, 20));
    }

    private void initializeUnstableRouteCompletedJobs() {
        createCompletedSampleJob("FOUP-UNSTABLE-001", "EQP_02", 82, LocalDateTime.of(2026, 6, 18, 10, 40));
        createCompletedSampleJob("FOUP-UNSTABLE-002", "EQP_02", 110, LocalDateTime.of(2026, 6, 18, 11, 10));
        createCompletedSampleJob("FOUP-UNSTABLE-003", "EQP_02", 145, LocalDateTime.of(2026, 6, 18, 11, 45));
        createCompletedSampleJob("FOUP-UNSTABLE-004", "EQP_02", 90, LocalDateTime.of(2026, 6, 18, 12, 10));
        createCompletedSampleJob("FOUP-UNSTABLE-005", "EQP_02", 175, LocalDateTime.of(2026, 6, 18, 13, 0));
        createCompletedSampleJob("FOUP-UNSTABLE-006", "EQP_02", 240, LocalDateTime.of(2026, 6, 18, 13, 40));
    }

    private void initializeFailureAnalyticsJobs() {
        createFailedSampleJob("FOUP-FAIL-001", "EQP_01", "EQUIPMENT_ERROR");
        createFailedSampleJob("FOUP-FAIL-002", "EQP_01", "EQUIPMENT_ERROR");
        createFailedSampleJob("FOUP-FAIL-003", "EQP_02", "EQUIPMENT_ERROR");
        createFailedSampleJob("FOUP-FAIL-004", "EQP_02", "NODE_BLOCKED");
        createFailedSampleJob("FOUP-FAIL-005", "EQP_01", "NODE_BLOCKED");
        createFailedSampleJob("FOUP-FAIL-006", "EQP_02", "EDGE_BLOCKED");
        createFailedSampleJob("FOUP-FAIL-007", "EQP_01", "NO_AVAILABLE_EQUIPMENT");
    }

    private void createCompletedSampleJob(String carrierId, String destinationNodeCode, int actualSeconds, LocalDateTime completedAt) {
        var created = transferJobService.createTransferJob(
                new TransferJobCreateRequest(
                        carrierId,
                        "STOCKER_01",
                        destinationNodeCode,
                        TransferJobPriority.NORMAL
                )
        );
        transferJobService.assignTransferJob(created.id());
        transferJobService.updateTransferJobStatus(
                created.id(),
                new TransferJobStatusUpdateRequest(TransferJobStatus.MOVING, "Sample moving", null)
        );
        transferJobService.updateTransferJobStatus(
                created.id(),
                new TransferJobStatusUpdateRequest(TransferJobStatus.COMPLETED, null, null)
        );

        LocalDateTime startedAt = completedAt.minusSeconds(actualSeconds);
        transferJobRepository.findById(created.id())
                .ifPresent(job -> job.overrideCompletionMetrics(startedAt, completedAt, actualSeconds));
    }

    private void createFailedSampleJob(String carrierId, String destinationNodeCode, String failureReason) {
        var created = transferJobService.createTransferJob(
                new TransferJobCreateRequest(
                        carrierId,
                        "STOCKER_01",
                        destinationNodeCode,
                        TransferJobPriority.NORMAL
                )
        );
        transferJobService.assignTransferJob(created.id());
        transferJobService.updateTransferJobStatus(
                created.id(),
                new TransferJobStatusUpdateRequest(TransferJobStatus.FAILED, failureReason, null)
        );
    }
}
