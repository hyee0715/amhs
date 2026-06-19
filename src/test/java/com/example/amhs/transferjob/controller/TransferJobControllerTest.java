package com.example.amhs.transferjob.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.amhs.alert.repository.AlertRepository;
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
import com.example.amhs.transferjob.repository.TransferJobHistoryRepository;
import com.example.amhs.transferjob.repository.TransferJobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TransferJobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransferJobHistoryRepository transferJobHistoryRepository;

    @Autowired
    private TransferJobRepository transferJobRepository;

    @Autowired
    private EdgeRepository edgeRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private NodeRepository nodeRepository;

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
    @DisplayName("Transfer Job 생성 API가 동작한다")
    void createTransferJob() throws Exception {
        mockMvc.perform(post("/api/transfer-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "carrierId": "FOUP-001",
                                  "sourceNodeCode": "STOCKER_01",
                                  "destinationNodeCode": "EQP_01",
                                  "priority": "NORMAL"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.path[1]").value("NODE_B"));
    }

    @Test
    @DisplayName("Transfer Job 상태 변경 API가 동작한다")
    void updateTransferJobStatus() throws Exception {
        String response = mockMvc.perform(post("/api/transfer-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "carrierId": "FOUP-001",
                                  "sourceNodeCode": "STOCKER_01",
                                  "destinationNodeCode": "EQP_01",
                                  "priority": "NORMAL"
                                }
                                """))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(response).path("id").asLong();

        mockMvc.perform(post("/api/transfer-jobs/{id}/assign", id))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/transfer-jobs/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "MOVING",
                                  "reason": "Job started"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MOVING"))
                .andExpect(jsonPath("$.assignedEquipmentCode").value("OHT_001"));
    }

    @Test
    @DisplayName("Transfer Job 이력 조회 API가 동작한다")
    void getTransferJobHistories() throws Exception {
        String response = mockMvc.perform(post("/api/transfer-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "carrierId": "FOUP-001",
                                  "sourceNodeCode": "STOCKER_01",
                                  "destinationNodeCode": "EQP_01",
                                  "priority": "NORMAL"
                                }
                                """))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(response).path("id").asLong();

        mockMvc.perform(get("/api/transfer-jobs/{id}/histories", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("CREATED"))
                .andExpect(jsonPath("$[0].pathSnapshot[0]").value("STOCKER_01"));
    }

    @Test
    @DisplayName("Transfer Job retry API가 동작한다")
    void retryTransferJob() throws Exception {
        String response = mockMvc.perform(post("/api/transfer-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "carrierId": "FOUP-001",
                                  "sourceNodeCode": "STOCKER_01",
                                  "destinationNodeCode": "EQP_01",
                                  "priority": "NORMAL"
                                }
                                """))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(response).path("id").asLong();

        mockMvc.perform(post("/api/transfer-jobs/{id}/assign", id))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/transfer-jobs/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "MOVING",
                                  "reason": "Job started"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/transfer-jobs/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "FAILED",
                                  "reason": "EDGE_BLOCKED"
                                }
                                """))
                .andExpect(status().isOk());

        AmhsEdge edge = edgeRepository.findAll()
                .stream()
                .filter(it -> it.getFromNode().getCode().equals("STOCKER_01")
                        && it.getToNode().getCode().equals("NODE_B"))
                .findFirst()
                .orElseThrow();
        edge.changeStatus(EdgeStatus.BLOCKED);
        edgeRepository.save(edge);

        mockMvc.perform(post("/api/transfer-jobs/{id}/retry", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.retryCount").value(1))
                .andExpect(jsonPath("$.path[1]").value("NODE_A"));
    }

    @Test
    @DisplayName("Transfer Job assign API가 동작한다")
    void assignTransferJob() throws Exception {
        String response = mockMvc.perform(post("/api/transfer-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "carrierId": "FOUP-001",
                                  "sourceNodeCode": "STOCKER_01",
                                  "destinationNodeCode": "EQP_01",
                                  "priority": "NORMAL"
                                }
                                """))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(response).path("id").asLong();

        mockMvc.perform(post("/api/transfer-jobs/{id}/assign", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.assignedEquipmentCode").value("OHT_001"));
    }

    @Test
    @DisplayName("Transfer Job assign-pending API가 우선순위 순서로 동작한다")
    void assignPendingTransferJobs() throws Exception {
        equipmentRepository.save(Equipment.create("OHT_002", "OHT 002", EquipmentType.OHT));

        mockMvc.perform(post("/api/transfer-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "carrierId": "FOUP-001",
                                  "sourceNodeCode": "STOCKER_01",
                                  "destinationNodeCode": "EQP_01",
                                  "priority": "NORMAL"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/transfer-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "carrierId": "FOUP-002",
                                  "sourceNodeCode": "STOCKER_01",
                                  "destinationNodeCode": "EQP_01",
                                  "priority": "URGENT"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/transfer-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "carrierId": "FOUP-003",
                                  "sourceNodeCode": "STOCKER_01",
                                  "destinationNodeCode": "EQP_01",
                                  "priority": "HIGH"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/transfer-jobs/assign-pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].carrierId").value("FOUP-002"))
                .andExpect(jsonPath("$[0].status").value("ASSIGNED"))
                .andExpect(jsonPath("$[1].carrierId").value("FOUP-003"))
                .andExpect(jsonPath("$[1].status").value("ASSIGNED"));
    }

    @Test
    @DisplayName("dispatch-candidates API가 우선순위 순서로 동작한다")
    void getDispatchCandidates() throws Exception {
        mockMvc.perform(post("/api/transfer-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "carrierId": "FOUP-001",
                                  "sourceNodeCode": "STOCKER_01",
                                  "destinationNodeCode": "EQP_01",
                                  "priority": "NORMAL"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/transfer-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "carrierId": "FOUP-002",
                                  "sourceNodeCode": "STOCKER_01",
                                  "destinationNodeCode": "EQP_01",
                                  "priority": "URGENT"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/transfer-jobs/dispatch-candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].carrierId").value("FOUP-002"))
                .andExpect(jsonPath("$[1].carrierId").value("FOUP-001"));
    }

    @Test
    @DisplayName("사용 가능한 장비가 없으면 assign API는 예외를 반환한다")
    void assignTransferJobWhenNoAvailableEquipment() throws Exception {
        var equipments = equipmentRepository.findAll();
        equipments.forEach(equipment -> equipment.changeStatus(EquipmentStatus.ERROR));
        equipmentRepository.saveAll(equipments);

        String response = mockMvc.perform(post("/api/transfer-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "carrierId": "FOUP-001",
                                  "sourceNodeCode": "STOCKER_01",
                                  "destinationNodeCode": "EQP_01",
                                  "priority": "NORMAL"
                                }
                                """))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(response).path("id").asLong();

        mockMvc.perform(post("/api/transfer-jobs/{id}/assign", id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NO_AVAILABLE_EQUIPMENT"));
    }

    @Test
    @DisplayName("허용되지 않은 상태 전이는 예외 응답을 반환한다")
    void invalidStatusTransition() throws Exception {
        String response = mockMvc.perform(post("/api/transfer-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "carrierId": "FOUP-001",
                                  "sourceNodeCode": "STOCKER_01",
                                  "destinationNodeCode": "EQP_01",
                                  "priority": "NORMAL"
                                }
                                """))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(response).path("id").asLong();

        mockMvc.perform(patch("/api/transfer-jobs/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "MOVING",
                                  "reason": "Job started"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JOB_STATUS_TRANSITION"));
    }
}
