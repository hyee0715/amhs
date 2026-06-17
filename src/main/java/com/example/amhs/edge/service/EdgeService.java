package com.example.amhs.edge.service;

import com.example.amhs.common.exception.BusinessException;
import com.example.amhs.common.exception.ErrorCode;
import com.example.amhs.edge.domain.AmhsEdge;
import com.example.amhs.edge.dto.EdgeCreateRequest;
import com.example.amhs.edge.dto.EdgeResponse;
import com.example.amhs.edge.dto.EdgeStatusUpdateRequest;
import com.example.amhs.edge.repository.EdgeRepository;
import com.example.amhs.node.domain.AmhsNode;
import com.example.amhs.node.repository.NodeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EdgeService {

    private final EdgeRepository edgeRepository;
    private final NodeRepository nodeRepository;

    @Transactional
    public List<EdgeResponse> createEdge(EdgeCreateRequest request) {
        AmhsNode fromNode = findNodeByCode(request.fromNodeCode());
        AmhsNode toNode = findNodeByCode(request.toNodeCode());

        validateDuplicateEdge(request.fromNodeCode(), request.toNodeCode());

        AmhsEdge edge = edgeRepository.save(
                AmhsEdge.create(fromNode, toNode, request.distance(), request.estimatedTimeSeconds())
        );

        if (!request.bidirectional()) {
            return List.of(EdgeResponse.from(edge));
        }

        validateDuplicateEdge(request.toNodeCode(), request.fromNodeCode());

        AmhsEdge reverseEdge = edgeRepository.save(
                AmhsEdge.create(toNode, fromNode, request.distance(), request.estimatedTimeSeconds())
        );

        return List.of(EdgeResponse.from(edge), EdgeResponse.from(reverseEdge));
    }

    public List<EdgeResponse> getEdges() {
        return edgeRepository.findAll()
                .stream()
                .map(EdgeResponse::from)
                .toList();
    }

    public EdgeResponse getEdge(Long id) {
        return EdgeResponse.from(findEdgeById(id));
    }

    @Transactional
    public EdgeResponse updateEdgeStatus(Long id, EdgeStatusUpdateRequest request) {
        AmhsEdge edge = findEdgeById(id);
        edge.changeStatus(request.status());
        return EdgeResponse.from(edge);
    }

    private void validateDuplicateEdge(String fromNodeCode, String toNodeCode) {
        if (edgeRepository.existsByFromNode_CodeAndToNode_Code(fromNodeCode, toNodeCode)) {
            throw new BusinessException(
                    ErrorCode.DUPLICATED_EDGE,
                    "Duplicated edge: " + fromNodeCode + " -> " + toNodeCode
            );
        }
    }

    private AmhsNode findNodeByCode(String code) {
        return nodeRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NODE_NOT_FOUND,
                        "Node not found: " + code
                ));
    }

    private AmhsEdge findEdgeById(Long id) {
        return edgeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.EDGE_NOT_FOUND,
                        "Edge not found: " + id
                ));
    }
}
