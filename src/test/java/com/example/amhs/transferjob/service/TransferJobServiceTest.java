package com.example.amhs.transferjob.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.amhs.alert.repository.AlertRepository;
import com.example.amhs.common.exception.BusinessException;
import com.example.amhs.common.exception.ErrorCode;
import com.example.amhs.edge.domain.AmhsEdge;
import com.example.amhs.edge.domain.EdgeStatus;
import com.example.amhs.edge.repository.EdgeRepository;
import com.example.amhs.equipment.domain.Equipment;
import com.example.amhs.equipment.domain.EquipmentStatus;
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
        transferJobService.assignTransferJob(created.id());

        TransferJobResponse updated = transferJobService.updateTransferJobStatus(
                created.id(),
                new TransferJobStatusUpdateRequest(TransferJobStatus.MOVING, "Job started", null)
        );

        List<TransferJobHistoryResponse> histories = transferJobService.getTransferJobHistories(created.id());

        assertThat(updated.status()).isEqualTo(TransferJobStatus.MOVING);
        assertThat(updated.assignedEquipmentCode()).isEqualTo("OHT_001");
        assertThat(histories).hasSize(3);
        assertThat(histories.get(2).status()).isEqualTo(TransferJobStatus.MOVING);
    }

    @Test
    @DisplayName("COMPLETED 상태가 되면 completedAt이 저장된다")
    void updateTransferJobCompleted() {
        TransferJobResponse created = transferJobService.createTransferJob(
                new TransferJobCreateRequest("FOUP-001", "STOCKER_01", "EQP_01",
                        TransferJobPriority.NORMAL)
        );
        transferJobService.assignTransferJob(created.id());
        transferJobService.updateTransferJobStatus(
                created.id(),
                new TransferJobStatusUpdateRequest(TransferJobStatus.MOVING, "Job started", null)
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
        transferJobService.assignTransferJob(created.id());
        transferJobService.updateTransferJobStatus(
                created.id(),
                new TransferJobStatusUpdateRequest(TransferJobStatus.MOVING, "Job started", null)
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

    @Test
    @DisplayName("FAILED 상태의 Job은 현재 경로 상태 기준으로 재시도할 수 있다")
    void retryTransferJob() {
        TransferJobResponse created = transferJobService.createTransferJob(
                new TransferJobCreateRequest("FOUP-001", "STOCKER_01", "EQP_01", TransferJobPriority.NORMAL)
        );
        transferJobService.assignTransferJob(created.id());
        transferJobService.updateTransferJobStatus(
                created.id(),
                new TransferJobStatusUpdateRequest(TransferJobStatus.MOVING, "Job started", null)
        );

        transferJobService.updateTransferJobStatus(
                created.id(),
                new TransferJobStatusUpdateRequest(TransferJobStatus.FAILED, "EDGE_BLOCKED", null)
        );

        AmhsEdge edge = edgeRepository.findAll()
                .stream()
                .filter(it -> it.getFromNode().getCode().equals("STOCKER_01")
                        && it.getToNode().getCode().equals("NODE_B"))
                .findFirst()
                .orElseThrow();
        edge.changeStatus(EdgeStatus.BLOCKED);
        edgeRepository.save(edge);

        TransferJobResponse retried = transferJobService.retryTransferJob(created.id());
        List<TransferJobHistoryResponse> histories = transferJobService.getTransferJobHistories(created.id());

        assertThat(retried.status()).isEqualTo(TransferJobStatus.CREATED);
        assertThat(retried.retryCount()).isEqualTo(1);
        assertThat(retried.path()).isEqualTo(List.of("STOCKER_01", "NODE_A", "EQP_01"));
        assertThat(retried.failureReason()).isNull();
        assertThat(histories).hasSize(5);
        assertThat(histories.get(4).status()).isEqualTo(TransferJobStatus.CREATED);
    }

    @Test
    @DisplayName("FAILED 상태가 아닌 Job은 재시도할 수 없다")
    void retryTransferJobWhenNotFailed() {
        TransferJobResponse created = transferJobService.createTransferJob(
                new TransferJobCreateRequest("FOUP-001", "STOCKER_01", "EQP_01", TransferJobPriority.NORMAL)
        );

        assertThatThrownBy(() -> transferJobService.retryTransferJob(created.id()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_JOB_STATUS);
    }

    @Test
    @DisplayName("재시도 시 현재 상태에서 경로가 없으면 ROUTE_NOT_FOUND 예외가 발생한다")
    void retryTransferJobWhenRouteNotFound() {
        TransferJobResponse created = transferJobService.createTransferJob(
                new TransferJobCreateRequest("FOUP-001", "STOCKER_01", "EQP_01", TransferJobPriority.NORMAL)
        );
        transferJobService.assignTransferJob(created.id());
        transferJobService.updateTransferJobStatus(
                created.id(),
                new TransferJobStatusUpdateRequest(TransferJobStatus.MOVING, "Job started", null)
        );

        transferJobService.updateTransferJobStatus(
                created.id(),
                new TransferJobStatusUpdateRequest(TransferJobStatus.FAILED, "EDGE_BLOCKED", null)
        );

        List<AmhsEdge> edges = edgeRepository.findAll();
        edges.forEach(edge -> edge.changeStatus(EdgeStatus.BLOCKED));
        edgeRepository.saveAll(edges);

        assertThatThrownBy(() -> transferJobService.retryTransferJob(created.id()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ROUTE_NOT_FOUND);
    }

    @Test
    @DisplayName("생성된 Job에 사용 가능한 장비를 자동 할당할 수 있다")
    void assignTransferJob() {
        TransferJobResponse created = transferJobService.createTransferJob(
                new TransferJobCreateRequest("FOUP-001", "STOCKER_01", "EQP_01", TransferJobPriority.NORMAL)
        );

        TransferJobResponse assigned = transferJobService.assignTransferJob(created.id());
        List<TransferJobHistoryResponse> histories = transferJobService.getTransferJobHistories(created.id());

        assertThat(assigned.status()).isEqualTo(TransferJobStatus.ASSIGNED);
        assertThat(assigned.assignedEquipmentCode()).isEqualTo("OHT_001");
        assertThat(equipmentRepository.findByCode("OHT_001").orElseThrow().getStatus())
                .isEqualTo(EquipmentStatus.MOVING);
        assertThat(histories).hasSize(2);
        assertThat(histories.get(1).status()).isEqualTo(TransferJobStatus.ASSIGNED);
        assertThat(histories.get(1).assignedEquipmentCode()).isEqualTo("OHT_001");
    }

    @Test
    @DisplayName("할당 가능한 장비가 없으면 NO_AVAILABLE_EQUIPMENT 예외가 발생한다")
    void assignTransferJobWhenNoAvailableEquipment() {
        List<Equipment> equipments = equipmentRepository.findAll();
        equipments.forEach(equipment -> equipment.changeStatus(EquipmentStatus.ERROR));
        equipmentRepository.saveAll(equipments);

        TransferJobResponse created = transferJobService.createTransferJob(
                new TransferJobCreateRequest("FOUP-001", "STOCKER_01", "EQP_01", TransferJobPriority.NORMAL)
        );

        assertThatThrownBy(() -> transferJobService.assignTransferJob(created.id()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.NO_AVAILABLE_EQUIPMENT);
    }

    @Test
    @DisplayName("일괄 할당 시 우선순위가 높은 Job부터 IDLE 장비에 배정된다")
    void assignPendingTransferJobs() {
        equipmentRepository.save(Equipment.create("OHT_002", "OHT 002", EquipmentType.OHT));

        TransferJobResponse normal = transferJobService.createTransferJob(
                new TransferJobCreateRequest("FOUP-001", "STOCKER_01", "EQP_01", TransferJobPriority.NORMAL)
        );
        TransferJobResponse urgent = transferJobService.createTransferJob(
                new TransferJobCreateRequest("FOUP-002", "STOCKER_01", "EQP_01", TransferJobPriority.URGENT)
        );
        TransferJobResponse high = transferJobService.createTransferJob(
                new TransferJobCreateRequest("FOUP-003", "STOCKER_01", "EQP_01", TransferJobPriority.HIGH)
        );

        List<TransferJobResponse> assigned = transferJobService.assignPendingTransferJobs();

        assertThat(assigned).hasSize(2);
        assertThat(assigned.get(0).id()).isEqualTo(urgent.id());
        assertThat(assigned.get(0).status()).isEqualTo(TransferJobStatus.ASSIGNED);
        assertThat(assigned.get(1).id()).isEqualTo(high.id());
        assertThat(assigned.get(1).status()).isEqualTo(TransferJobStatus.ASSIGNED);
        assertThat(transferJobService.getTransferJob(normal.id()).status()).isEqualTo(TransferJobStatus.CREATED);
    }

    @Test
    @DisplayName("작업 완료 또는 실패 시 할당 장비는 다시 IDLE 상태가 된다")
    void releaseEquipmentWhenTerminalStatus() {
        TransferJobResponse created = transferJobService.createTransferJob(
                new TransferJobCreateRequest("FOUP-001", "STOCKER_01", "EQP_01", TransferJobPriority.NORMAL)
        );
        transferJobService.assignTransferJob(created.id());
        transferJobService.updateTransferJobStatus(
                created.id(),
                new TransferJobStatusUpdateRequest(TransferJobStatus.MOVING, "Job started", null)
        );

        transferJobService.updateTransferJobStatus(
                created.id(),
                new TransferJobStatusUpdateRequest(TransferJobStatus.COMPLETED, null, null)
        );

        assertThat(equipmentRepository.findByCode("OHT_001").orElseThrow().getStatus())
                .isEqualTo(EquipmentStatus.IDLE);
    }

    @Test
    @DisplayName("허용되지 않은 상태 전이는 INVALID_JOB_STATUS_TRANSITION 예외가 발생한다")
    void invalidStatusTransition() {
        TransferJobResponse created = transferJobService.createTransferJob(
                new TransferJobCreateRequest("FOUP-001", "STOCKER_01", "EQP_01", TransferJobPriority.NORMAL)
        );

        assertThatThrownBy(() -> transferJobService.updateTransferJobStatus(
                created.id(),
                new TransferJobStatusUpdateRequest(TransferJobStatus.MOVING, "Job started", null)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_JOB_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("완료된 Job은 다시 MOVING으로 변경할 수 없다")
    void completedJobCannotMoveAgain() {
        TransferJobResponse created = transferJobService.createTransferJob(
                new TransferJobCreateRequest("FOUP-001", "STOCKER_01", "EQP_01", TransferJobPriority.NORMAL)
        );
        transferJobService.assignTransferJob(created.id());
        transferJobService.updateTransferJobStatus(
                created.id(),
                new TransferJobStatusUpdateRequest(TransferJobStatus.MOVING, "Job started", null)
        );
        transferJobService.updateTransferJobStatus(
                created.id(),
                new TransferJobStatusUpdateRequest(TransferJobStatus.COMPLETED, null, null)
        );

        assertThatThrownBy(() -> transferJobService.updateTransferJobStatus(
                created.id(),
                new TransferJobStatusUpdateRequest(TransferJobStatus.MOVING, "Restart", null)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_JOB_STATUS_TRANSITION);
    }
}
