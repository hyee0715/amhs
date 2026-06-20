package com.example.amhs.analytics.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.amhs.alert.repository.AlertRepository;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
    @DisplayName("이상 지연 Job 탐지 API가 IQR 기반 결과를 반환한다")
    void getTransferTimeOutliers() throws Exception {
        createCompletedJob("FOUP-001", "EQP_01", 10);
        createCompletedJob("FOUP-002", "EQP_01", 11);
        createCompletedJob("FOUP-003", "EQP_01", 12);
        createCompletedJob("FOUP-004", "EQP_01", 13);
        createCompletedJob("FOUP-005", "EQP_02", 60);

        mockMvc.perform(get("/api/analytics/transfer-time/outliers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobCount").value(5))
                .andExpect(jsonPath("$.outlierCount").value(1))
                .andExpect(jsonPath("$.outlierJobs[0].carrierId").value("FOUP-005"))
                .andExpect(jsonPath("$.outlierJobs[0].path[2]").value("EQP_02"));
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
