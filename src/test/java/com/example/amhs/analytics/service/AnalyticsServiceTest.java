package com.example.amhs.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.amhs.alert.repository.AlertRepository;
import com.example.amhs.analytics.dto.TransferTimeOutlierResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class AnalyticsServiceTest {

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
        AmhsNode eqp01 = nodeRepository.save(AmhsNode.create("EQP_01", "Equipment 01", NodeType.EQP));
        AmhsNode eqp02 = nodeRepository.save(AmhsNode.create("EQP_02", "Equipment 02", NodeType.EQP));

        edgeRepository.save(AmhsEdge.create(stocker, nodeA, 100, 10));
        edgeRepository.save(AmhsEdge.create(nodeA, eqp01, 100, 10));
        edgeRepository.save(AmhsEdge.create(nodeA, eqp02, 100, 10));

        equipmentRepository.save(Equipment.create("OHT_001", "OHT 001", EquipmentType.OHT));
        equipmentRepository.save(Equipment.create("OHT_002", "OHT 002", EquipmentType.OHT));
    }

    @Test
    @DisplayName("완료된 Job으로 IQR과 outlier threshold를 계산한다")
    void getTransferTimeOutliersCalculatesIqr() {
        createCompletedJob("FOUP-001", "EQP_01", 40);
        createCompletedJob("FOUP-002", "EQP_01", 42);
        createCompletedJob("FOUP-003", "EQP_01", 44);
        createCompletedJob("FOUP-004", "EQP_01", 46);

        TransferTimeOutlierResponse response = analyticsService.getTransferTimeOutliers();

        assertThat(response.jobCount()).isEqualTo(4);
        assertThat(response.q1()).isEqualTo(41.5);
        assertThat(response.q3()).isEqualTo(44.5);
        assertThat(response.iqr()).isEqualTo(3.0);
        assertThat(response.outlierThreshold()).isEqualTo(49.0);
        assertThat(response.outlierCount()).isEqualTo(0);
        assertThat(response.outlierJobs()).isEmpty();
    }

    @Test
    @DisplayName("Upper fence를 초과하는 완료 Job을 이상 지연 Job으로 탐지한다")
    void getTransferTimeOutliersDetectsDelayedJobs() {
        createCompletedJob("FOUP-001", "EQP_01", 40);
        createCompletedJob("FOUP-002", "EQP_01", 42);
        createCompletedJob("FOUP-003", "EQP_01", 44);
        createCompletedJob("FOUP-004", "EQP_01", 46);
        createCompletedJob("FOUP-005", "EQP_02", 210);
        createCompletedJob("FOUP-006", "EQP_02", 95);

        TransferTimeOutlierResponse response = analyticsService.getTransferTimeOutliers();

        assertThat(response.jobCount()).isEqualTo(6);
        assertThat(response.q1()).isEqualTo(42.5);
        assertThat(response.q3()).isEqualTo(82.75);
        assertThat(response.iqr()).isEqualTo(40.25);
        assertThat(response.outlierThreshold()).isEqualTo(143.13);
        assertThat(response.outlierCount()).isEqualTo(1);
        assertThat(response.outlierJobs()).hasSize(1);
        assertThat(response.outlierJobs().get(0).carrierId()).isEqualTo("FOUP-005");
        assertThat(response.outlierJobs().get(0).actualTransferTimeSeconds()).isEqualTo(210);
    }

    @Test
    @DisplayName("데이터가 4건 미만이면 IQR 관련 값은 null로 반환한다")
    void getTransferTimeOutliersReturnsNullWhenInsufficientData() {
        createCompletedJob("FOUP-001", "EQP_01", 40);
        createCompletedJob("FOUP-002", "EQP_01", 42);
        createCompletedJob("FOUP-003", "EQP_01", 120);

        TransferTimeOutlierResponse response = analyticsService.getTransferTimeOutliers();

        assertThat(response.jobCount()).isEqualTo(3);
        assertThat(response.q1()).isNull();
        assertThat(response.q3()).isNull();
        assertThat(response.iqr()).isNull();
        assertThat(response.outlierThreshold()).isNull();
        assertThat(response.outlierCount()).isEqualTo(0);
        assertThat(response.outlierJobs()).isEmpty();
    }

    @Test
    @DisplayName("이상 지연 Job은 actualTransferTimeSeconds 내림차순으로 반환된다")
    void getTransferTimeOutliersSortsOutlierJobsDescending() {
        createCompletedJob("FOUP-001", "EQP_01", 10);
        createCompletedJob("FOUP-002", "EQP_01", 11);
        createCompletedJob("FOUP-003", "EQP_01", 12);
        createCompletedJob("FOUP-004", "EQP_01", 13);
        createCompletedJob("FOUP-005", "EQP_01", 14);
        createCompletedJob("FOUP-006", "EQP_01", 15);
        var firstOutlierId = createCompletedJob("FOUP-007", "EQP_02", 60);
        var secondOutlierId = createCompletedJob("FOUP-008", "EQP_02", 90);

        TransferTimeOutlierResponse response = analyticsService.getTransferTimeOutliers();

        assertThat(response.outlierCount()).isEqualTo(2);
        assertThat(response.outlierJobs()).extracting("jobId").containsExactly(secondOutlierId, firstOutlierId);
        assertThat(response.outlierJobs()).extracting("actualTransferTimeSeconds").containsExactly(90, 60);
        assertThat(response.outlierJobs().get(0).path()).containsExactly("STOCKER_01", "NODE_A", "EQP_02");
    }

    private Long createCompletedJob(String carrierId, String destinationNodeCode, int actualTransferTimeSeconds) {
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
        return created.id();
    }
}
