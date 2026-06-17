package com.example.amhs.edge.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.amhs.edge.repository.EdgeRepository;
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
class EdgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EdgeRepository edgeRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private TransferJobHistoryRepository transferJobHistoryRepository;

    @Autowired
    private TransferJobRepository transferJobRepository;

    @BeforeEach
    void setUp() {
        transferJobHistoryRepository.deleteAll();
        transferJobRepository.deleteAll();
        edgeRepository.deleteAll();
        nodeRepository.deleteAll();

        nodeRepository.save(AmhsNode.create("STOCKER_01", "Stocker 01", NodeType.STOCKER));
        nodeRepository.save(AmhsNode.create("NODE_A", "Node A", NodeType.OHT_NODE));
        nodeRepository.save(AmhsNode.create("EQP_01", "Equipment 01", NodeType.EQP));
    }

    @Test
    @DisplayName("Edge를 등록할 수 있다")
    void createEdge() throws Exception {
        mockMvc.perform(post("/api/edges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fromNodeCode": "STOCKER_01",
                                  "toNodeCode": "NODE_A",
                                  "distance": 100,
                                  "estimatedTimeSeconds": 30,
                                  "bidirectional": false
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].fromNodeCode").value("STOCKER_01"))
                .andExpect(jsonPath("$[0].toNodeCode").value("NODE_A"));
    }

    @Test
    @DisplayName("Edge 상태를 변경할 수 있다")
    void updateEdgeStatus() throws Exception {
        String response = mockMvc.perform(post("/api/edges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fromNodeCode": "STOCKER_01",
                                  "toNodeCode": "NODE_A",
                                  "distance": 100,
                                  "estimatedTimeSeconds": 30,
                                  "bidirectional": false
                                }
                                """))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(response).get(0).path("id").asLong();

        mockMvc.perform(patch("/api/edges/{id}/status", id)
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
    @DisplayName("Edge 목록 조회가 가능하다")
    void getEdges() throws Exception {
        mockMvc.perform(post("/api/edges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fromNodeCode": "STOCKER_01",
                                  "toNodeCode": "NODE_A",
                                  "distance": 100,
                                  "estimatedTimeSeconds": 30,
                                  "bidirectional": false
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/edges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fromNodeCode").value("STOCKER_01"));
    }

    @Test
    @DisplayName("Edge 단건 조회가 가능하다")
    void getEdge() throws Exception {
        String response = mockMvc.perform(post("/api/edges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fromNodeCode": "STOCKER_01",
                                  "toNodeCode": "NODE_A",
                                  "distance": 100,
                                  "estimatedTimeSeconds": 30,
                                  "bidirectional": false
                                }
                                """))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(response).get(0).path("id").asLong();

        mockMvc.perform(get("/api/edges/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromNodeCode").value("STOCKER_01"));
    }

    @Test
    @DisplayName("없는 Edge를 조회하면 예외가 발생한다")
    void getEdgeWithInvalidId() throws Exception {
        mockMvc.perform(get("/api/edges/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("EDGE_NOT_FOUND"));
    }
}
