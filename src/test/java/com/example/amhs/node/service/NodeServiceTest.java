package com.example.amhs.node.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.amhs.common.exception.BusinessException;
import com.example.amhs.common.exception.ErrorCode;
import com.example.amhs.alert.repository.AlertRepository;
import com.example.amhs.node.domain.NodeStatus;
import com.example.amhs.node.domain.NodeType;
import com.example.amhs.node.dto.NodeCreateRequest;
import com.example.amhs.node.dto.NodeResponse;
import com.example.amhs.node.dto.NodeStatusUpdateRequest;
import com.example.amhs.node.repository.NodeRepository;
import com.example.amhs.transferjob.repository.TransferJobHistoryRepository;
import com.example.amhs.transferjob.repository.TransferJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class NodeServiceTest {

    @Autowired
    private NodeService nodeService;

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
    @DisplayName("서비스에서 노드를 등록하면 기본 상태는 AVAILABLE이다")
    void createNode() {
        NodeResponse response = nodeService.createNode(
                new NodeCreateRequest("STOCKER_01", "Stocker 01", NodeType.STOCKER)
        );

        assertThat(response.id()).isNotNull();
        assertThat(response.code()).isEqualTo("STOCKER_01");
        assertThat(response.status()).isEqualTo(NodeStatus.AVAILABLE);
    }

    @Test
    @DisplayName("중복 code로 노드를 등록하면 DUPLICATED_NODE_CODE 예외가 발생한다")
    void createNodeWithDuplicateCode() {
        nodeService.createNode(new NodeCreateRequest("NODE_A", "Node A", NodeType.OHT_NODE));

        assertThatThrownBy(() ->
                nodeService.createNode(new NodeCreateRequest("NODE_A", "Another Node A", NodeType.OHT_NODE))
        )
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATED_NODE_CODE);
    }

    @Test
    @DisplayName("없는 노드를 조회하면 NODE_NOT_FOUND 예외가 발생한다")
    void getNodeWithInvalidId() {
        assertThatThrownBy(() -> nodeService.getNode(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.NODE_NOT_FOUND);
    }

    @Test
    @DisplayName("노드 상태를 변경할 수 있다")
    void updateNodeStatus() {
        NodeResponse created = nodeService.createNode(
                new NodeCreateRequest("EQP_01", "Equipment 01", NodeType.EQP)
        );

        NodeResponse updated = nodeService.updateNodeStatus(
                created.id(),
                new NodeStatusUpdateRequest(NodeStatus.BLOCKED)
        );

        assertThat(updated.status()).isEqualTo(NodeStatus.BLOCKED);
        assertThat(nodeRepository.findById(created.id()))
                .get()
                .extracting(node -> node.getStatus())
                .isEqualTo(NodeStatus.BLOCKED);
    }

    @Test
    @DisplayName("노드 목록을 조회할 수 있다")
    void getNodes() {
        nodeService.createNode(new NodeCreateRequest("NODE_A", "Node A", NodeType.OHT_NODE));
        nodeService.createNode(new NodeCreateRequest("NODE_B", "Node B", NodeType.OHT_NODE));

        assertThat(nodeService.getNodes()).hasSize(2);
    }
}
