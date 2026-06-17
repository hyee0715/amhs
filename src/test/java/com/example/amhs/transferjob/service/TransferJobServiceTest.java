package com.example.amhs.transferjob.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.amhs.common.exception.BusinessException;
import com.example.amhs.common.exception.ErrorCode;
import com.example.amhs.edge.domain.AmhsEdge;
import com.example.amhs.edge.repository.EdgeRepository;
import com.example.amhs.equipment.domain.Equipment;
import com.example.amhs.equipment.domain.EquipmentType;
import com.example.amhs.equipment.repository.EquipmentRepository;
import com.example.amhs.node.domain.AmhsNode;
import com.example.amhs.node.domain.NodeType;
import com.example.amhs.node.repository.NodeRepository;
import com.example.amhs.transferjob.domain.TransferJobPriority;
import com.example.amhs.transferjob.domain.TransferJobStatus;
import com.example.amhs.transferjob.dto.TransferJobCreateRequest;
import com.example.amhs.transferjob.dto.TransferJobHistoryResponse;
import com.example.amhs.transferjob.dto.TransferJobResponse;
import com.example.amhs.transferjob.dto.TransferJobStatusUpdateRequest;
import com.example.amhs.transferjob.repository.TransferJobHistoryRepository;
import com.example.amhs.transferjob.repository.TransferJobRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TransferJobServiceTest {

    @Autowired
    private TransferJobService transferJobService;

    @Autowired
    private TransferJobRepository transferJobRepository;

    @Autowired
    private TransferJobHistoryRepository transferJobHistoryRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private EdgeRepository edgeRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @BeforeEach
    void setUp() {
        transferJobHistoryRepository.deleteAll();
        transferJobRepository.deleteAll();
        edgeRepository.deleteAll();
        equipmentRepository.deleteAll();
        nodeRepository.deleteAll();

        AmhsNode stocker = nodeRepository.save(AmhsNode.create("STOCKER_01", "Stocker 01", NodeType.STOCKER));
        AmhsNode nodeA = nodeRepository.save(AmhsNode.create("NODE_A", "Node A", NodeType.OHT_NODE));
        AmhsNode nodeB = nodeRepository.save(AmhsNode.create("NODE_B", "Node B", NodeType.OHT_NODE));
        AmhsNode eqp = nodeRepository.save(AmhsNode.create("EQP_01", "Equipment 01", NodeType.EQP));

        edgeRepository.save(AmhsEdge.create(stocker, nodeA, 100, 10));
        edgeRepository.save(AmhsEdge.create(nodeA, eqp, 100, 10));
        edgeRepository.save(AmhsEdge.create(stocker, nodeB, 50, 5));
        edgeRepository.save(AmhsEdge.create(nodeB, eqp, 50, 5));

        equipmentRepository.save(Equipment.create("OHT_001", "OHT 001", EquipmentType.OHT));
    }

    @Test
    @DisplayName("Transfer Job 생성 시 Dijkstra 경로와 예상 시간이 저장된다")
    void createTransferJob() {
        TransferJobResponse response = transferJobService.createTransferJob(
                new TransferJobCreateRequest("FOUP-001", "STOCKER_01", "EQP_01",
                        TransferJobPriority.NORMAL)
        );

        assertThat(response.status()).isEqualTo(TransferJobStatus.CREATED);
        assertThat(response.path()).isEqualTo(List.of("STOCKER_01", "NODE_B", "EQP_01"));
        assertThat(response.estimatedTimeSeconds()).isEqualTo(10);
        assertThat(transferJobHistoryRepository.findByTransferJobIdOrderByCreatedAtAsc(response.id())).hasSize(1);
    }

    @Test
    @DisplayName("Transfer Job 상태 변경 시 이력이 저장된다")
    void updateTransferJobStatus() {
        TransferJobResponse created = transferJobService.createTransferJob(
                new TransferJobCreateRequest("FOUP-001", "STOCKER_01", "EQP_01",
                        TransferJobPriority.NORMAL)
        );

        TransferJobResponse updated = transferJobService.updateTransferJobStatus(
                created.id(),
                new TransferJobStatusUpdateRequest(TransferJobStatus.MOVING, "Job started", "OHT_001")
        );

        List<TransferJobHistoryResponse> histories = transferJobService.getTransferJobHistories(created.id());

        assertThat(updated.status()).isEqualTo(TransferJobStatus.MOVING);
        assertThat(updated.assignedEquipmentCode()).isEqualTo("OHT_001");
        assertThat(histories).hasSize(2);
        assertThat(histories.get(1).status()).isEqualTo(TransferJobStatus.MOVING);
    }

    @Test
    @DisplayName("COMPLETED 상태가 되면 completedAt이 저장된다")
    void updateTransferJobCompleted() {
        TransferJobResponse created = transferJobService.createTransferJob(
                new TransferJobCreateRequest("FOUP-001", "STOCKER_01", "EQP_01",
                        TransferJobPriority.NORMAL)
        );

        TransferJobResponse updated = transferJobService.updateTransferJobStatus(
                created.id(),
                new TransferJobStatusUpdateRequest(TransferJobStatus.COMPLETED, null, null)
        );

        assertThat(updated.completedAt()).isNotNull();
    }

    @Test
    @DisplayName("FAILED 상태가 되면 failedAt과 failureReason이 저장된다")
    void updateTransferJobFailed() {
        TransferJobResponse created = transferJobService.createTransferJob(
                new TransferJobCreateRequest("FOUP-001", "STOCKER_01", "EQP_01",
                        TransferJobPriority.NORMAL)
        );

        TransferJobResponse updated = transferJobService.updateTransferJobStatus(
                created.id(),
                new TransferJobStatusUpdateRequest(TransferJobStatus.FAILED, "NODE_BLOCKED", null)
        );

        assertThat(updated.failedAt()).isNotNull();
        assertThat(updated.failureReason()).isEqualTo("NODE_BLOCKED");
    }

    @Test
    @DisplayName("FAILED 상태에 reason이 없으면 INVALID_JOB_STATUS 예외가 발생한다")
    void updateTransferJobFailedWithoutReason() {
        TransferJobResponse created = transferJobService.createTransferJob(
                new TransferJobCreateRequest("FOUP-001", "STOCKER_01", "EQP_01",
                        TransferJobPriority.NORMAL)
        );

        assertThatThrownBy(() -> transferJobService.updateTransferJobStatus(
                created.id(),
                new TransferJobStatusUpdateRequest(TransferJobStatus.FAILED, null, null)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_JOB_STATUS);
    }
}
