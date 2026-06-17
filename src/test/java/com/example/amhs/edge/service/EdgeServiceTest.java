package com.example.amhs.edge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.amhs.common.exception.BusinessException;
import com.example.amhs.common.exception.ErrorCode;
import com.example.amhs.edge.domain.EdgeStatus;
import com.example.amhs.edge.dto.EdgeCreateRequest;
import com.example.amhs.edge.dto.EdgeResponse;
import com.example.amhs.edge.dto.EdgeStatusUpdateRequest;
import com.example.amhs.edge.repository.EdgeRepository;
import com.example.amhs.node.domain.AmhsNode;
import com.example.amhs.node.domain.NodeType;
import com.example.amhs.node.repository.NodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EdgeServiceTest {

    @Autowired
    private EdgeService edgeService;

    @Autowired
    private EdgeRepository edgeRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @BeforeEach
    void setUp() {
        edgeRepository.deleteAll();
        nodeRepository.deleteAll();

        nodeRepository.save(AmhsNode.create("STOCKER_01", "Stocker 01", NodeType.STOCKER));
        nodeRepository.save(AmhsNode.create("NODE_A", "Node A", NodeType.OHT_NODE));
        nodeRepository.save(AmhsNode.create("EQP_01", "Equipment 01", NodeType.EQP));
    }

    @Test
    @DisplayName("단방향 Edge를 등록할 수 있다")
    void createEdge() {
        var responses = edgeService.createEdge(
                new EdgeCreateRequest("STOCKER_01", "NODE_A", 100, 30, false)
        );

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().fromNodeCode()).isEqualTo("STOCKER_01");
        assertThat(responses.getFirst().toNodeCode()).isEqualTo("NODE_A");
        assertThat(responses.getFirst().status()).isEqualTo(EdgeStatus.AVAILABLE);
    }

    @Test
    @DisplayName("bidirectional이 true면 역방향 Edge도 함께 생성된다")
    void createBidirectionalEdge() {
        var responses = edgeService.createEdge(
                new EdgeCreateRequest("STOCKER_01", "NODE_A", 100, 30, true)
        );

        assertThat(responses).hasSize(2);
        assertThat(edgeRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("중복 Edge를 등록하면 DUPLICATED_EDGE 예외가 발생한다")
    void createDuplicateEdge() {
        edgeService.createEdge(new EdgeCreateRequest("STOCKER_01", "NODE_A", 100, 30, false));

        assertThatThrownBy(() ->
                edgeService.createEdge(new EdgeCreateRequest("STOCKER_01", "NODE_A", 120, 40, false))
        )
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATED_EDGE);
    }

    @Test
    @DisplayName("없는 Node code로 Edge를 등록하면 NODE_NOT_FOUND 예외가 발생한다")
    void createEdgeWithInvalidNodeCode() {
        assertThatThrownBy(() ->
                edgeService.createEdge(new EdgeCreateRequest("UNKNOWN", "NODE_A", 100, 30, false))
        )
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.NODE_NOT_FOUND);
    }

    @Test
    @DisplayName("Edge 상태를 변경할 수 있다")
    void updateEdgeStatus() {
        EdgeResponse created = edgeService.createEdge(
                new EdgeCreateRequest("STOCKER_01", "NODE_A", 100, 30, false)
        ).getFirst();

        EdgeResponse updated = edgeService.updateEdgeStatus(
                created.id(),
                new EdgeStatusUpdateRequest(EdgeStatus.BLOCKED)
        );

        assertThat(updated.status()).isEqualTo(EdgeStatus.BLOCKED);
    }

    @Test
    @DisplayName("없는 Edge를 조회하면 EDGE_NOT_FOUND 예외가 발생한다")
    void getEdgeWithInvalidId() {
        assertThatThrownBy(() -> edgeService.getEdge(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.EDGE_NOT_FOUND);
    }
}
