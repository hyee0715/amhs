package com.example.amhs.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.amhs.alert.repository.AlertRepository;
import com.example.amhs.analytics.dto.RouteStabilityLevel;
import com.example.amhs.analytics.dto.RouteStabilityResponse;
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
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class RouteStabilityAnalyticsServiceTest {

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        AmhsNode eqp01 = nodeRepository.save(AmhsNode.create("EQP_01", "Equipment 01", NodeType.EQP));
        AmhsNode eqp02 = nodeRepository.save(AmhsNode.create("EQP_02", "Equipment 02", NodeType.EQP));
        AmhsNode eqp03 = nodeRepository.save(AmhsNode.create("EQP_03", "Equipment 03", NodeType.EQP));

        edgeRepository.save(AmhsEdge.create(stocker, nodeA, 100, 10));
        edgeRepository.save(AmhsEdge.create(nodeA, eqp01, 100, 10));
        edgeRepository.save(AmhsEdge.create(stocker, nodeB, 120, 12));
        edgeRepository.save(AmhsEdge.create(nodeB, eqp02, 120, 12));
        edgeRepository.save(AmhsEdge.create(stocker, eqp03, 80, 8));

        equipmentRepository.save(Equipment.create("OHT_001", "OHT 001", EquipmentType.OHT));
        equipmentRepository.save(Equipment.create("OHT_002", "OHT 002", EquipmentType.OHT));
        equipmentRepository.save(Equipment.create("OHT_003", "OHT 003", EquipmentType.OHT));
    }

    @Test
    @DisplayName("경로별 평균과 표준편차를 계산한다")
    void getRouteStabilitiesCalculatesAverageAndStandardDeviation() {
        createCompletedJob("FOUP-001", "EQP_01", 20);
        createCompletedJob("FOUP-002", "EQP_01", 22);
        createCompletedJob("FOUP-003", "EQP_01", 24);

        List<RouteStabilityResponse> responses = analyticsService.getRouteStabilities();

        RouteStabilityResponse route = responses.stream()
                .filter(response -> response.route().equals(List.of("STOCKER_01", "NODE_A", "EQP_01")))
                .findFirst()
                .orElseThrow();

        assertThat(route.jobCount()).isEqualTo(3);
        assertThat(route.averageTransferTimeSeconds()).isEqualTo(22.0);
        assertThat(route.standardDeviation()).isEqualTo(1.63);
        assertThat(route.coefficientOfVariation()).isEqualTo(0.07);
        assertThat(route.minTransferTimeSeconds()).isEqualTo(20);
        assertThat(route.maxTransferTimeSeconds()).isEqualTo(24);
        assertThat(route.stability()).isEqualTo(RouteStabilityLevel.STABLE);
    }

    @Test
    @DisplayName("CV 기준으로 UNSTABLE 경로를 분류하고 우선 정렬한다")
    void getRouteStabilitiesClassifiesUnstableRoutes() {
        createCompletedJob("FOUP-001", "EQP_01", 20);
        createCompletedJob("FOUP-002", "EQP_01", 22);
        createCompletedJob("FOUP-003", "EQP_01", 24);

        createCompletedJob("FOUP-004", "EQP_02", 20);
        createCompletedJob("FOUP-005", "EQP_02", 60);
        createCompletedJob("FOUP-006", "EQP_02", 100);

        createCompletedJob("FOUP-007", "EQP_03", 30);
        createCompletedJob("FOUP-008", "EQP_03", 40);
        createCompletedJob("FOUP-009", "EQP_03", 50);

        List<RouteStabilityResponse> responses = analyticsService.getRouteStabilities();

        assertThat(responses.get(0).route()).isEqualTo(List.of("STOCKER_01", "NODE_B", "EQP_02"));
        assertThat(responses.get(0).stability()).isEqualTo(RouteStabilityLevel.UNSTABLE);
        assertThat(responses.get(0).coefficientOfVariation()).isEqualTo(0.54);

        assertThat(responses.get(1).route()).isEqualTo(List.of("STOCKER_01", "EQP_03"));
        assertThat(responses.get(1).stability()).isEqualTo(RouteStabilityLevel.MODERATE);
        assertThat(responses.get(1).coefficientOfVariation()).isEqualTo(0.2);

        assertThat(responses.get(2).route()).isEqualTo(List.of("STOCKER_01", "NODE_A", "EQP_01"));
        assertThat(responses.get(2).stability()).isEqualTo(RouteStabilityLevel.STABLE);
    }

    @Test
    @DisplayName("jobCount가 1개인 경로는 표준편차와 CV를 0으로 반환한다")
    void getRouteStabilitiesReturnsZeroForSingleJobRoute() {
        createCompletedJob("FOUP-001", "EQP_03", 30);

        List<RouteStabilityResponse> responses = analyticsService.getRouteStabilities();

        RouteStabilityResponse route = responses.get(0);
        assertThat(route.jobCount()).isEqualTo(1);
        assertThat(route.standardDeviation()).isEqualTo(0.0);
        assertThat(route.coefficientOfVariation()).isEqualTo(0.0);
        assertThat(route.stability()).isEqualTo(RouteStabilityLevel.STABLE);
    }

    private void createCompletedJob(String carrierId, String destinationNodeCode, int actualTransferTimeSeconds) {
        var created = transferJobService.createTransferJob(
                new TransferJobCreateRequest(carrierId, "STOCKER_01", destinationNodeCode, TransferJobPriority.NORMAL)
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

        LocalDateTime completedAt = LocalDateTime.now();
        LocalDateTime startedAt = completedAt.minusSeconds(actualTransferTimeSeconds);
        jdbcTemplate.update(
                "update transfer_jobs set started_at = ?, completed_at = ?, actual_transfer_time_seconds = ? where id = ?",
                startedAt,
                completedAt,
                actualTransferTimeSeconds,
                created.id()
        );
    }
}
