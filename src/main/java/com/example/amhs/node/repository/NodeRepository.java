package com.example.amhs.node.repository;

import com.example.amhs.node.domain.AmhsNode;
import com.example.amhs.node.domain.NodeStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NodeRepository extends JpaRepository<AmhsNode, Long> {

    boolean existsByCode(String code);

    Optional<AmhsNode> findByCode(String code);

    long countByStatus(NodeStatus status);
}
