package com.example.amhs.route.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.amhs.edge.domain.AmhsEdge;
import com.example.amhs.edge.repository.EdgeRepository;
import com.example.amhs.node.domain.AmhsNode;
import com.example.amhs.node.domain.NodeType;
import com.example.amhs.node.repository.NodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RouteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private EdgeRepository edgeRepository;

    @BeforeEach
    void setUp() {
        edgeRepository.deleteAll();
        nodeRepository.deleteAll();

        AmhsNode stocker = nodeRepository.save(AmhsNode.create("STOCKER_01", "Stocker 01", NodeType.STOCKER));
        AmhsNode nodeA = nodeRepository.save(AmhsNode.create("NODE_A", "Node A", NodeType.OHT_NODE));
        AmhsNode nodeB = nodeRepository.save(AmhsNode.create("NODE_B", "Node B", NodeType.OHT_NODE));
        AmhsNode nodeC = nodeRepository.save(AmhsNode.create("NODE_C", "Node C", NodeType.OHT_NODE));
        AmhsNode eqp = nodeRepository.save(AmhsNode.create("EQP_01", "Equipment 01", NodeType.EQP));

        edgeRepository.save(AmhsEdge.create(stocker, nodeA, 100, 10));
        edgeRepository.save(AmhsEdge.create(nodeA, eqp, 100, 10));
        edgeRepository.save(AmhsEdge.create(stocker, nodeB, 80, 5));
        edgeRepository.save(AmhsEdge.create(nodeB, nodeC, 80, 5));
        edgeRepository.save(AmhsEdge.create(nodeC, eqp, 80, 5));
    }

    @Test
    @DisplayName("BFS 경로 조회 API가 동작한다")
    void getBfsRoute() throws Exception {
        mockMvc.perform(get("/api/routes/bfs")
                        .param("source", "STOCKER_01")
                        .param("destination", "EQP_01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("BFS"))
                .andExpect(jsonPath("$.path[0]").value("STOCKER_01"))
                .andExpect(jsonPath("$.path[2]").value("EQP_01"));
    }

    @Test
    @DisplayName("Dijkstra 경로 조회 API가 동작한다")
    void getDijkstraRoute() throws Exception {
        mockMvc.perform(get("/api/routes/dijkstra")
                        .param("source", "STOCKER_01")
                        .param("destination", "EQP_01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("DIJKSTRA"))
                .andExpect(jsonPath("$.totalEstimatedTimeSeconds").value(15));
    }

    @Test
    @DisplayName("경로가 없으면 ROUTE_NOT_FOUND 응답을 반환한다")
    void getRouteNotFound() throws Exception {
        edgeRepository.deleteAll();

        mockMvc.perform(get("/api/routes/dijkstra")
                        .param("source", "STOCKER_01")
                        .param("destination", "EQP_01"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("ROUTE_NOT_FOUND"));
    }
}
