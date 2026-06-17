package com.example.amhs.node.service;

import com.example.amhs.common.exception.BusinessException;
import com.example.amhs.common.exception.ErrorCode;
import com.example.amhs.node.domain.AmhsNode;
import com.example.amhs.node.dto.NodeCreateRequest;
import com.example.amhs.node.dto.NodeResponse;
import com.example.amhs.node.dto.NodeStatusUpdateRequest;
import com.example.amhs.node.repository.NodeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NodeService {

    private final NodeRepository nodeRepository;

    @Transactional
    public NodeResponse createNode(NodeCreateRequest request) {
        validateDuplicateCode(request.code());

        AmhsNode node = AmhsNode.create(request.code(), request.name(), request.type());
        return NodeResponse.from(nodeRepository.save(node));
    }

    public List<NodeResponse> getNodes() {
        return nodeRepository.findAll()
                .stream()
                .map(NodeResponse::from)
                .toList();
    }

    public NodeResponse getNode(Long id) {
        return NodeResponse.from(findNodeById(id));
    }

    @Transactional
    public NodeResponse updateNodeStatus(Long id, NodeStatusUpdateRequest request) {
        AmhsNode node = findNodeById(id);
        node.changeStatus(request.status());
        return NodeResponse.from(node);
    }

    private void validateDuplicateCode(String code) {
        if (nodeRepository.existsByCode(code)) {
            throw new BusinessException(
                    ErrorCode.DUPLICATED_NODE_CODE,
                    "Duplicated node code: " + code
            );
        }
    }

    private AmhsNode findNodeById(Long id) {
        return nodeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NODE_NOT_FOUND,
                        "Node not found: " + id
                ));
    }
}
