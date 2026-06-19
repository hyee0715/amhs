package com.example.amhs.dashboard.service;

import com.example.amhs.alert.repository.AlertRepository;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.amhs.dashboard.dto.DashboardSummaryResponse;
import com.example.amhs.edge.domain.AmhsEdge;
import com.example.amhs.edge.domain.EdgeStatus;
import com.example.amhs.edge.repository.EdgeRepository;
import com.example.amhs.equipment.domain.Equipment;
import com.example.amhs.equipment.domain.EquipmentStatus;
import com.example.amhs.equipment.domain.EquipmentType;
import com.example.amhs.equipment.repository.EquipmentRepository;
import com.example.amhs.node.domain.AmhsNode;
import com.example.amhs.node.domain.NodeStatus;
import com.example.amhs.node.domain.NodeType;
import com.example.amhs.node.repository.NodeRepository;
import com.example.amhs.transferjob.domain.TransferJobPriority;
import com.example.amhs.transferjob.domain.TransferJobStatus;
import com.example.amhs.transferjob.dto.TransferJobCreateRequest;
import com.example.amhs.transferjob.dto.TransferJobStatusUpdateRequest;
import com.example.amhs.transferjob.repository.TransferJobHistoryRepository;
import com.example.amhs.transferjob.repository.TransferJobRepository;
import com.example.amhs.transferjob.service.TransferJobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DashboardServiceTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private TransferJobService transferJobService;

    @Autowired
    private TransferJobHistoryRepository transferJobHistoryRepository;

    @Autowired
    private TransferJobRepository transferJobRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private EdgeRepository edgeRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private AlertRepository alertRepository;

    @BeforeEach
    void setUp() {
        alertRepository.deleteAll();
        transferJobHistoryRepository.deleteAll();
        transferJobRepository.deleteAll();
        edgeRepository.deleteAll();
        equipmentRepository.deleteAll();
        nodeRepository.deleteAll();

        AmhsNode stocker = nodeRepository.save(AmhsNode.create("STOCKER_01", "Stocker 01", NodeType.STOCKER));
        AmhsNode nodeA = nodeRepository.save(AmhsNode.create("NODE_A", "Node A", NodeType.OHT_NODE));
        AmhsNode nodeB = nodeRepository.save(AmhsNode.create("NODE_B", "Node B", NodeType.OHT_NODE));
        AmhsNode eqp = nodeRepository.save(AmhsNode.create("EQP_01", "Equipment 01", NodeType.EQP));
        AmhsNode blockedNode = nodeRepository.save(AmhsNode.create("NODE_C", "Node C", NodeType.OHT_NODE));
        blockedNode.changeStatus(NodeStatus.BLOCKED);
        nodeRepository.save(blockedNode);

        edgeRepository.save(AmhsEdge.create(stocker, nodeA, 100, 10));
        edgeRepository.save(AmhsEdge.create(nodeA, eqp, 100, 10));
        edgeRepository.save(AmhsEdge.create(stocker, nodeB, 50, 5));
        edgeRepository.save(AmhsEdge.create(nodeB, eqp, 50, 5));

        AmhsEdge blockedEdge = edgeRepository.save(AmhsEdge.create(nodeA, nodeB, 70, 7));
        blockedEdge.changeStatus(EdgeStatus.BLOCKED);
        edgeRepository.save(blockedEdge);

        equipmentRepository.save(Equipment.create("OHT_001", "OHT 001", EquipmentType.OHT));
        Equipment errorEquipment = equipmentRepository.save(
                Equipment.create("ROBOT_001", "Robot 001", EquipmentType.ROBOT)
        );
        errorEquipment.changeStatus(EquipmentStatus.ERROR);
        equipmentRepository.save(errorEquipment);

        var created = transferJobService.createTransferJob(
                new TransferJobCreateRequest("FOUP-001", "STOCKER_01", "EQP_01", TransferJobPriority.NORMAL)
        );
        transferJobService.assignTransferJob(created.id());
        transferJobService.updateTransferJobStatus(
                created.id(),
                new TransferJobStatusUpdateRequest(TransferJobStatus.MOVING, "Job started", null)
        );
        transferJobService.createTransferJob(
                new TransferJobCreateRequest("FOUP-002", "STOCKER_01", "EQP_01", TransferJobPriority.HIGH)
        );

        transferJobService.updateTransferJobStatus(
                created.id(),
                new TransferJobStatusUpdateRequest(TransferJobStatus.COMPLETED, null, null)
        );

        var failed = transferJobService.createTransferJob(
                new TransferJobCreateRequest("FOUP-003", "STOCKER_01", "EQP_01", TransferJobPriority.URGENT)
        );
        transferJobService.assignTransferJob(failed.id());
        transferJobService.updateTransferJobStatus(
                failed.id(),
                new TransferJobStatusUpdateRequest(TransferJobStatus.MOVING, "Job started", null)
        );
        transferJobService.updateTransferJobStatus(
                failed.id(),
                new TransferJobStatusUpdateRequest(TransferJobStatus.FAILED, "EDGE_BLOCKED", null)
        );

        var moving = transferJobService.createTransferJob(
                new TransferJobCreateRequest("FOUP-004", "STOCKER_01", "EQP_01", TransferJobPriority.NORMAL)
        );
        transferJobService.assignTransferJob(moving.id());
        transferJobService.updateTransferJobStatus(
                moving.id(),
                new TransferJobStatusUpdateRequest(TransferJobStatus.MOVING, "Job started", null)
        );
    }

    @Test
    @DisplayName("Dashboard summary를 조회할 수 있다")
    void getSummary() {
        DashboardSummaryResponse summary = dashboardService.getSummary();

        assertThat(summary.totalJobs()).isEqualTo(4);
        assertThat(summary.createdJobs()).isEqualTo(1);
        assertThat(summary.movingJobs()).isEqualTo(1);
        assertThat(summary.completedJobs()).isEqualTo(1);
        assertThat(summary.failedJobs()).isEqualTo(1);
        assertThat(summary.successRate()).isEqualTo(25.0);
        assertThat(summary.averageEstimatedTimeSeconds()).isEqualTo(10.0);
        assertThat(summary.blockedNodeCount()).isEqualTo(1);
        assertThat(summary.blockedEdgeCount()).isEqualTo(1);
        assertThat(summary.equipmentErrorCount()).isEqualTo(1);
    }
}
