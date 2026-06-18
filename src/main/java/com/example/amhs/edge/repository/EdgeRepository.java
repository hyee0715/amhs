package com.example.amhs.edge.repository;

import com.example.amhs.edge.domain.AmhsEdge;
import com.example.amhs.edge.domain.EdgeStatus;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EdgeRepository extends JpaRepository<AmhsEdge, Long> {

    boolean existsByFromNode_CodeAndToNode_Code(String fromNodeCode, String toNodeCode);

    @EntityGraph(attributePaths = {"fromNode", "toNode"})
    List<AmhsEdge> findAll();

    long countByStatus(EdgeStatus status);
}
