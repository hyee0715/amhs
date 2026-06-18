package com.example.amhs.route.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.amhs.alert.repository.AlertRepository;
import com.example.amhs.common.exception.BusinessException;
import com.example.amhs.common.exception.ErrorCode;
import com.example.amhs.edge.domain.AmhsEdge;
import com.example.amhs.edge.domain.EdgeStatus;
import com.example.amhs.edge.repository.EdgeRepository;
import com.example.amhs.node.domain.AmhsNode;
import com.example.amhs.node.domain.NodeStatus;
import com.example.amhs.node.domain.NodeType;
import com.example.amhs.node.repository.NodeRepository;
import com.example.amhs.route.dto.RouteResult;
import com.example.amhs.transferjob.repository.TransferJobHistoryRepository;
import com.example.amhs.transferjob.repository.TransferJobRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RouteServiceTest {

    @Autowired
    private RouteService routeService;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private EdgeRepository edgeRepository;

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
        edgeRepository.deleteAll();
        nodeRepository.deleteAll();

        AmhsNode stocker = nodeRepository.save(AmhsNode.create("STOCKER_01", "Stocker 01", NodeType.STOCKER));
        AmhsNode nodeA = nodeRepository.save(AmhsNode.create("NODE_A", "Node A", NodeType.OHT_NODE));
        AmhsNode nodeB = nodeRepository.save(AmhsNode.create("NODE_B", "Node B", NodeType.OHT_NODE));
        AmhsNode nodeC = nodeRepository.save(AmhsNode.create("NODE_C", "Node C", NodeType.OHT_NODE));
        AmhsNode nodeD = nodeRepository.save(AmhsNode.create("NODE_D", "Node D", NodeType.OHT_NODE));
        AmhsNode eqp = nodeRepository.save(AmhsNode.create("EQP_01", "Equipment 01", NodeType.EQP));

        edgeRepository.save(AmhsEdge.create(stocker, nodeA, 100, 10));
        edgeRepository.save(AmhsEdge.create(nodeA, eqp, 100, 10));

        edgeRepository.save(AmhsEdge.create(stocker, nodeB, 80, 5));
        edgeRepository.save(AmhsEdge.create(nodeB, nodeC, 80, 5));
        edgeRepository.save(AmhsEdge.create(nodeC, eqp, 80, 5));

        edgeRepository.save(AmhsEdge.create(stocker, nodeD, 50, 3));
    }

    @Test
    @DisplayName("BFS는 거쳐가는 노드 수가 가장 적은 경로를 찾는다")
    void findRouteByBfs() {
        RouteResult result = routeService.findRouteByBfs("STOCKER_01", "EQP_01");

        assertThat(result.algorithm()).isEqualTo("BFS");
        assertThat(result.path()).isEqualTo(List.of("STOCKER_01", "NODE_A", "EQP_01"));
        assertThat(result.totalEstimatedTimeSeconds()).isEqualTo(20);
        assertThat(result.totalDistance()).isEqualTo(200);
    }

    @Test
    @DisplayName("Dijkstra는 예상 시간이 가장 짧은 경로를 찾는다")
    void findRouteByDijkstra() {
        RouteResult result = routeService.findRouteByDijkstra("STOCKER_01", "EQP_01");

        assertThat(result.algorithm()).isEqualTo("DIJKSTRA");
        assertThat(result.path()).isEqualTo(List.of("STOCKER_01", "NODE_B", "NODE_C", "EQP_01"));
        assertThat(result.totalEstimatedTimeSeconds()).isEqualTo(15);
        assertThat(result.totalDistance()).isEqualTo(240);
    }

    @Test
    @DisplayName("BLOCKED Node는 제외하고 우회 경로를 탐색한다")
    void findRouteExcludingBlockedNode() {
        AmhsNode nodeB = nodeRepository.findByCode("NODE_B").orElseThrow();
        nodeB.changeStatus(NodeStatus.BLOCKED);
        nodeRepository.save(nodeB);

        RouteResult result = routeService.findRouteByDijkstra("STOCKER_01", "EQP_01");

        assertThat(result.path()).isEqualTo(List.of("STOCKER_01", "NODE_A", "EQP_01"));
    }

    @Test
    @DisplayName("BLOCKED Edge는 제외하고 우회 경로를 탐색한다")
    void findRouteExcludingBlockedEdge() {
        AmhsEdge edge = edgeRepository.findAll()
                .stream()
                .filter(it -> it.getFromNode().getCode().equals("STOCKER_01")
                        && it.getToNode().getCode().equals("NODE_B"))
                .findFirst()
                .orElseThrow();
        edge.changeStatus(EdgeStatus.BLOCKED);
        edgeRepository.save(edge);

        RouteResult result = routeService.findRouteByDijkstra("STOCKER_01", "EQP_01");

        assertThat(result.path()).isEqualTo(List.of("STOCKER_01", "NODE_A", "EQP_01"));
    }

    @Test
    @DisplayName("경로가 없으면 ROUTE_NOT_FOUND 예외가 발생한다")
    void throwExceptionWhenRouteNotFound() {
        edgeRepository.deleteAll();

        assertThatThrownBy(() -> routeService.findRouteByDijkstra("STOCKER_01", "EQP_01"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ROUTE_NOT_FOUND);
    }
}
