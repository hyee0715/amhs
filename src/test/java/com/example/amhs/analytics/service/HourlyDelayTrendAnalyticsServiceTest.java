package com.example.amhs.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.amhs.alert.repository.AlertRepository;
import com.example.amhs.analytics.dto.HourlyDelayTrendResponse;
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
class HourlyDelayTrendAnalyticsServiceTest {

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
        AmhsNode eqp = nodeRepository.save(AmhsNode.create("EQP_01", "Equipment 01", NodeType.EQP));

        edgeRepository.save(AmhsEdge.create(stocker, nodeA, 100, 10));
        edgeRepository.save(AmhsEdge.create(nodeA, eqp, 100, 10));

        equipmentRepository.save(Equipment.create("OHT_001", "OHT 001", EquipmentType.OHT));
        equipmentRepository.save(Equipment.create("OHT_002", "OHT 002", EquipmentType.OHT));
        equipmentRepository.save(Equipment.create("OHT_003", "OHT 003", EquipmentType.OHT));
    }

    @Test
    @DisplayName("시간대별 totalCompletedJobs, delayedJobs, delayRate, movingAverageDelayRate를 계산한다")
    void getHourlyDelayTrendsCalculatesMetrics() {
        createCompletedJob("FOUP-001", 9, 8, 10);
        createCompletedJob("FOUP-002", 9, 12, 10);
        createCompletedJob("FOUP-003", 10, 8, 10);
        createCompletedJob("FOUP-004", 10, 14, 10);
        createCompletedJob("FOUP-005", 11, 13, 10);
        createCompletedJob("FOUP-006", 11, 15, 10);

        List<HourlyDelayTrendResponse> responses = analyticsService.getHourlyDelayTrends();

        assertThat(responses).extracting(HourlyDelayTrendResponse::hour).containsExactly(9, 10, 11);

        assertThat(responses.get(0).totalCompletedJobs()).isEqualTo(2);
        assertThat(responses.get(0).delayedJobs()).isEqualTo(1);
        assertThat(responses.get(0).delayRate()).isEqualTo(50.0);
        assertThat(responses.get(0).movingAverageDelayRate()).isEqualTo(50.0);

        assertThat(responses.get(1).totalCompletedJobs()).isEqualTo(2);
        assertThat(responses.get(1).delayedJobs()).isEqualTo(1);
        assertThat(responses.get(1).delayRate()).isEqualTo(50.0);
        assertThat(responses.get(1).movingAverageDelayRate()).isEqualTo(50.0);

        assertThat(responses.get(2).totalCompletedJobs()).isEqualTo(2);
        assertThat(responses.get(2).delayedJobs()).isEqualTo(2);
        assertThat(responses.get(2).delayRate()).isEqualTo(100.0);
        assertThat(responses.get(2).movingAverageDelayRate()).isEqualTo(66.67);
    }

    @Test
    @DisplayName("데이터가 있는 시간대만 hour 오름차순으로 반환한다")
    void getHourlyDelayTrendsSortsByHourAsc() {
        createCompletedJob("FOUP-001", 15, 12, 10);
        createCompletedJob("FOUP-002", 3, 8, 10);
        createCompletedJob("FOUP-003", 11, 12, 10);

        List<HourlyDelayTrendResponse> responses = analyticsService.getHourlyDelayTrends();

        assertThat(responses).extracting(HourlyDelayTrendResponse::hour).containsExactly(3, 11, 15);
    }

    private void createCompletedJob(String carrierId, int completedHour, int actualSeconds, int estimatedSeconds) {
        var created = transferJobService.createTransferJob(
                new TransferJobCreateRequest(carrierId, "STOCKER_01", "EQP_01", TransferJobPriority.NORMAL)
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

        LocalDateTime completedAt = LocalDateTime.of(2026, 6, 18, completedHour, 0);
        LocalDateTime startedAt = completedAt.minusSeconds(actualSeconds);
        jdbcTemplate.update(
                "update transfer_jobs set started_at = ?, completed_at = ?, actual_transfer_time_seconds = ?, estimated_time_seconds = ? where id = ?",
                startedAt,
                completedAt,
                actualSeconds,
                estimatedSeconds,
                created.id()
        );
    }
}
