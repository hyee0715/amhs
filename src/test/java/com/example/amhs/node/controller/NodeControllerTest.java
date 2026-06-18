package com.example.amhs.node.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.amhs.alert.repository.AlertRepository;
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
class NodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private TransferJobHistoryRepository transferJobHistoryRepository;

    @Autowired
    private TransferJobRepository transferJobRepository;

    @Autowired
    private AlertRepository alertRepository;

    @BeforeEach
    void setUp() {
        alertRepository.deleteAll();
        transferJobHistoryRepository.deleteAll();
        transferJobRepository.deleteAll();
        nodeRepository.deleteAll();
    }

    @Test
    @DisplayName("노드를 등록할 수 있다")
    void createNode() throws Exception {
        String request = """
                {
                  "code": "STOCKER_01",
                  "name": "Stocker 01",
                  "type": "STOCKER"
                }
                """;

        mockMvc.perform(post("/api/nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("STOCKER_01"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("중복 code로 노드를 등록하면 예외가 발생한다")
    void createNodeWithDuplicateCode() throws Exception {
        String request = """
                {
                  "code": "NODE_A",
                  "name": "Node A",
                  "type": "OHT_NODE"
                }
                """;

        mockMvc.perform(post("/api/nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATED_NODE_CODE"));
    }

    @Test
    @DisplayName("노드 상태를 변경할 수 있다")
    void updateNodeStatus() throws Exception {
        String createRequest = """
                {
                  "code": "EQP_01",
                  "name": "Equipment 01",
                  "type": "EQP"
                }
                """;

        String response = mockMvc.perform(post("/api/nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(response).path("id").asLong();

        mockMvc.perform(patch("/api/nodes/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "BLOCKED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    @DisplayName("노드 단건 조회가 가능하다")
    void getNode() throws Exception {
        String createRequest = """
                {
                  "code": "PORT_01",
                  "name": "Port 01",
                  "type": "PORT"
                }
                """;

        String response = mockMvc.perform(post("/api/nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(response).path("id").asLong();

        mockMvc.perform(get("/api/nodes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PORT_01"));
    }

    @Test
    @DisplayName("노드 목록 조회가 가능하다")
    void getNodes() throws Exception {
        mockMvc.perform(post("/api/nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "NODE_A",
                                  "name": "Node A",
                                  "type": "OHT_NODE"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/nodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("NODE_A"));
    }

    @Test
    @DisplayName("없는 노드를 조회하면 예외가 발생한다")
    void getNodeWithInvalidId() throws Exception {
        mockMvc.perform(get("/api/nodes/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("NODE_NOT_FOUND"));
    }
}
