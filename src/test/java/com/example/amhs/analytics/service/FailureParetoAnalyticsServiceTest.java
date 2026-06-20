package com.example.amhs.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.amhs.alert.repository.AlertRepository;
import com.example.amhs.analytics.dto.FailureParetoResponse;
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
import com.example.amhs.transferjob.dto.TransferJobStatusUpdateRequest;
import com.example.amhs.transferjob.repository.TransferJobHistoryRepository;
import com.example.amhs.transferjob.repository.TransferJobRepository;
import com.example.amhs.transferjob.service.TransferJobService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class FailureParetoAnalyticsServiceTest {

    @Autowired
    private AnalyticsService analyticsService;

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
        AmhsNode eqp = nodeRepository.save(AmhsNode.create("EQP_01", "Equipment 01", NodeType.EQP));

        edgeRepository.save(AmhsEdge.create(stocker, nodeA, 100, 10));
        edgeRepository.save(AmhsEdge.create(nodeA, eqp, 100, 10));

        equipmentRepository.save(Equipment.create("OHT_001", "OHT 001", EquipmentType.OHT));
    }

    @Test
    @DisplayName("실패 원인별 count, ratio, cumulativeRatio를 계산한다")
    void getFailureParetoCalculatesParetoMetrics() {
        createFailedJob("FOUP-001", "EQUIPMENT_ERROR");
        createFailedJob("FOUP-002", "EQUIPMENT_ERROR");
        createFailedJob("FOUP-003", "NODE_BLOCKED");
        createFailedJob("FOUP-004", "NODE_BLOCKED");
        createFailedJob("FOUP-005", "EDGE_BLOCKED");

        List<FailureParetoResponse> responses = analyticsService.getFailurePareto();

        assertThat(responses).hasSize(3);
        assertThat(responses.get(0).failureReason()).isEqualTo("EQUIPMENT_ERROR");
        assertThat(responses.get(0).count()).isEqualTo(2);
        assertThat(responses.get(0).ratio()).isEqualTo(40.0);
        assertThat(responses.get(0).cumulativeRatio()).isEqualTo(40.0);

        assertThat(responses.get(1).failureReason()).isEqualTo("NODE_BLOCKED");
        assertThat(responses.get(1).count()).isEqualTo(2);
        assertThat(responses.get(1).ratio()).isEqualTo(40.0);
        assertThat(responses.get(1).cumulativeRatio()).isEqualTo(80.0);

        assertThat(responses.get(2).failureReason()).isEqualTo("EDGE_BLOCKED");
        assertThat(responses.get(2).count()).isEqualTo(1);
        assertThat(responses.get(2).ratio()).isEqualTo(20.0);
        assertThat(responses.get(2).cumulativeRatio()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("count 내림차순, count가 같으면 failureReason 오름차순으로 정렬한다")
    void getFailureParetoSortsByCountAndReason() {
        createFailedJob("FOUP-001", "NODE_BLOCKED");
        createFailedJob("FOUP-002", "EDGE_BLOCKED");
        createFailedJob("FOUP-003", "EQUIPMENT_ERROR");
        createFailedJob("FOUP-004", "EQUIPMENT_ERROR");

        List<FailureParetoResponse> responses = analyticsService.getFailurePareto();

        assertThat(responses).extracting(FailureParetoResponse::failureReason)
                .containsExactly("EQUIPMENT_ERROR", "EDGE_BLOCKED", "NODE_BLOCKED");
    }

    @Test
    @DisplayName("실패 데이터가 없으면 빈 배열을 반환한다")
    void getFailureParetoReturnsEmptyWhenNoFailedJobs() {
        assertThat(analyticsService.getFailurePareto()).isEmpty();
    }

    private void createFailedJob(String carrierId, String failureReason) {
        var created = transferJobService.createTransferJob(
                new TransferJobCreateRequest(carrierId, "STOCKER_01", "EQP_01", TransferJobPriority.NORMAL)
        );
        transferJobService.assignTransferJob(created.id());
        transferJobService.updateTransferJobStatus(
                created.id(),
                new TransferJobStatusUpdateRequest(TransferJobStatus.FAILED, failureReason, null)
        );
    }
}
