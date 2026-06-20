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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class FailureParetoAnalyticsControllerTest {

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
    @DisplayName("실패 원인 파레토 API가 집계 결과를 반환한다")
    void getFailurePareto() throws Exception {
        createFailedJob("FOUP-001", "EQUIPMENT_ERROR");
        createFailedJob("FOUP-002", "EQUIPMENT_ERROR");
        createFailedJob("FOUP-003", "NODE_BLOCKED");

        mockMvc.perform(get("/api/analytics/failures/pareto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].failureReason").value("EQUIPMENT_ERROR"))
                .andExpect(jsonPath("$[0].count").value(2))
                .andExpect(jsonPath("$[0].ratio").value(66.67))
                .andExpect(jsonPath("$[1].failureReason").value("NODE_BLOCKED"))
                .andExpect(jsonPath("$[1].cumulativeRatio").value(100.0));
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
