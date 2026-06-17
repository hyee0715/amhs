package com.example.amhs.node.repository;

import com.example.amhs.node.domain.AmhsNode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NodeRepository extends JpaRepository<AmhsNode, Long> {

    boolean existsByCode(String code);
}
