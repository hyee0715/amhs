package com.example.amhs.e2e;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.amhs.alert.service.AlertService;
import com.example.amhs.edge.domain.AmhsEdge;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ScenarioE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AlertService alertService;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private EdgeRepository edgeRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private TransferJobRepository transferJobRepository;

    @Autowired
    private TransferJobHistoryRepository transferJobHistoryRepository;

    @Autowired
    private com.example.amhs.alert.repository.AlertRepository alertRepository;

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

        ReflectionTestUtils.setField(alertService, "assignedThresholdMinutes", 5L);
        ReflectionTestUtils.setField(alertService, "movingGraceSeconds", 0L);

        AmhsNode stocker = nodeRepository.save(AmhsNode.create("STOCKER_01", "Stocker 01", NodeType.STOCKER));
        AmhsNode nodeA = nodeRepository.save(AmhsNode.create("NODE_A", "Node A", NodeType.OHT_NODE));
        AmhsNode nodeB = nodeRepository.save(AmhsNode.create("NODE_B", "Node B", NodeType.OHT_NODE));
        AmhsNode eqp = nodeRepository.save(AmhsNode.create("EQP_01", "Equipment 01", NodeType.EQP));

        edgeRepository.save(AmhsEdge.create(stocker, nodeA, 100, 10));
        edgeRepository.save(AmhsEdge.create(nodeA, eqp, 100, 10));
        edgeRepository.save(AmhsEdge.create(stocker, nodeB, 50, 5));
        edgeRepository.save(AmhsEdge.create(nodeB, eqp, 50, 5));

        equipmentRepository.save(Equipment.create("OHT_001", "OHT 001", EquipmentType.OHT));
        equipmentRepository.save(Equipment.create("OHT_002", "OHT 002", EquipmentType.OHT));
    }

    @Test
    @DisplayName("시나리오 1. 정상 반송 흐름")
    void scenario1NormalTransfer() throws Exception {
        Long jobId = createTransferJob("FOUP-001", "NORMAL");

        mockMvc.perform(post("/api/transfer-jobs/{id}/assign", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.assignedEquipmentCode").value("OHT_001"));

        mockMvc.perform(patch("/api/transfer-jobs/{id}/status", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "MOVING",
                                  "reason": "Job started"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MOVING"));

        mockMvc.perform(patch("/api/transfer-jobs/{id}/status", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "COMPLETED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").exists());

        mockMvc.perform(get("/api/transfer-jobs/{id}/histories", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("CREATED"))
                .andExpect(jsonPath("$[1].status").value("ASSIGNED"))
                .andExpect(jsonPath("$[2].status").value("MOVING"))
                .andExpect(jsonPath("$[3].status").value("COMPLETED"));
    }

    @Test
    @DisplayName("시나리오 2. 장애 우회 경로 계산")
    void scenario2BlockedEdgeDetour() throws Exception {
        Long edgeId = findEdgeId("STOCKER_01", "NODE_B");

        mockMvc.perform(patch("/api/edges/{id}/status", edgeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "BLOCKED"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/routes/dijkstra")
                        .param("source", "STOCKER_01")
                        .param("destination", "EQP_01")
                        .param("strategy", "TIME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path[0]").value("STOCKER_01"))
                .andExpect(jsonPath("$.path[1]").value("NODE_A"))
                .andExpect(jsonPath("$.path[2]").value("EQP_01"));

        mockMvc.perform(post("/api/transfer-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "carrierId": "FOUP-002",
                                  "sourceNodeCode": "STOCKER_01",
                                  "destinationNodeCode": "EQP_01",
                                  "priority": "NORMAL"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path[1]").value("NODE_A"));
    }

    @Test
    @DisplayName("시나리오 3. 실패 후 재처리")
    void scenario3FailAndRetry() throws Exception {
        Long jobId = createTransferJob("FOUP-003", "NORMAL");

        mockMvc.perform(post("/api/transfer-jobs/{id}/assign", jobId))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/transfer-jobs/{id}/status", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "MOVING",
                                  "reason": "Job started"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/transfer-jobs/{id}/status", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "FAILED",
                                  "reason": "EDGE_BLOCKED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));

        Long edgeId = findEdgeId("STOCKER_01", "NODE_B");
        mockMvc.perform(patch("/api/edges/{id}/status", edgeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "BLOCKED"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/transfer-jobs/{id}/retry", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.retryCount").value(1))
                .andExpect(jsonPath("$.path[1]").value("NODE_A"));
    }

    @Test
    @DisplayName("시나리오 4. 지연 감지 및 알림 해제")
    void scenario4DelayedAlert() throws Exception {
        Long jobId = createTransferJob("FOUP-004", "NORMAL");

        mockMvc.perform(post("/api/transfer-jobs/{id}/assign", jobId))
                .andExpect(status().isOk());

        jdbcTemplate.update(
                "update transfer_jobs set updated_at = ? where id = ?",
                LocalDateTime.now().minusMinutes(10),
                jobId
        );

        alertService.detectStuckAssignedJobs();

        String alertsResponse = mockMvc.perform(get("/api/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("STUCK_JOB"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long alertId = objectMapper.readTree(alertsResponse).get(0).path("id").asLong();

        mockMvc.perform(patch("/api/alerts/{id}/resolve", alertId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    @DisplayName("시나리오 5. 혼잡도 반영 경로 비교")
    void scenario5CongestionAwareRouting() throws Exception {
        Long edge1 = findEdgeId("STOCKER_01", "NODE_B");
        Long edge2 = findEdgeId("NODE_B", "EQP_01");

        mockMvc.perform(patch("/api/edges/{id}/congestion", edge1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "congestionLevel": 80
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/edges/{id}/congestion", edge2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "congestionLevel": 80
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/routes/dijkstra")
                        .param("source", "STOCKER_01")
                        .param("destination", "EQP_01")
                        .param("strategy", "TIME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("DIJKSTRA_TIME"))
                .andExpect(jsonPath("$.path[1]").value("NODE_B"));

        mockMvc.perform(get("/api/routes/dijkstra")
                        .param("source", "STOCKER_01")
                        .param("destination", "EQP_01")
                        .param("strategy", "CONGESTION_AWARE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("DIJKSTRA_CONGESTION_AWARE"))
                .andExpect(jsonPath("$.path[1]").value("NODE_A"));
    }

    @Test
    @DisplayName("추가 시나리오 1. 허용되지 않은 상태 전이 차단")
    void extraScenarioInvalidStatusTransition() throws Exception {
        Long jobId = createTransferJob("FOUP-005", "NORMAL");

        mockMvc.perform(patch("/api/transfer-jobs/{id}/status", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "MOVING",
                                  "reason": "Invalid direct move"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JOB_STATUS_TRANSITION"));
    }

    @Test
    @DisplayName("추가 시나리오 2. 동일 지연 알림은 중복 생성되지 않는다")
    void extraScenarioAlertDeduplication() throws Exception {
        Long jobId = createTransferJob("FOUP-006", "NORMAL");

        mockMvc.perform(post("/api/transfer-jobs/{id}/assign", jobId))
                .andExpect(status().isOk());

        jdbcTemplate.update(
                "update transfer_jobs set updated_at = ? where id = ?",
                LocalDateTime.now().minusMinutes(10),
                jobId
        );

        alertService.detectStuckAssignedJobs();
        alertService.detectStuckAssignedJobs();

        mockMvc.perform(get("/api/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("STUCK_JOB"))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    @DisplayName("추가 시나리오 3. 장비 오류 발생 시 Job 실패 및 Alert 생성")
    void extraScenarioEquipmentErrorFailure() throws Exception {
        Long jobId = createTransferJob("FOUP-007", "NORMAL");

        mockMvc.perform(post("/api/transfer-jobs/{id}/assign", jobId))
                .andExpect(status().isOk());

        Equipment equipment = equipmentRepository.findByCode("OHT_001").orElseThrow();
        equipment.changeStatus(EquipmentStatus.ERROR);
        equipmentRepository.save(equipment);

        alertService.detectEquipmentErrorJobs();

        mockMvc.perform(get("/api/transfer-jobs/{id}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("EQUIPMENT_ERROR"));

        mockMvc.perform(get("/api/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("EQUIPMENT_ERROR"))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    private Long createTransferJob(String carrierId, String priority) throws Exception {
        String response = mockMvc.perform(post("/api/transfer-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "carrierId": "%s",
                                  "sourceNodeCode": "STOCKER_01",
                                  "destinationNodeCode": "EQP_01",
                                  "priority": "%s"
                                }
                                """.formatted(carrierId, priority)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        return root.path("id").asLong();
    }

    private Long findEdgeId(String fromNodeCode, String toNodeCode) {
        return edgeRepository.findAll().stream()
                .filter(edge -> edge.getFromNode().getCode().equals(fromNodeCode))
                .filter(edge -> edge.getToNode().getCode().equals(toNodeCode))
                .findFirst()
                .orElseThrow()
                .getId();
    }
}
