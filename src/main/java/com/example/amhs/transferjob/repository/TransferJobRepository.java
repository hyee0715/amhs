package com.example.amhs.transferjob.repository;

import com.example.amhs.transferjob.domain.TransferJob;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferJobRepository extends JpaRepository<TransferJob, Long> {

    @Override
    @EntityGraph(attributePaths = {"sourceNode", "destinationNode", "assignedEquipment"})
    List<TransferJob> findAll();

    @Override
    @EntityGraph(attributePaths = {"sourceNode", "destinationNode", "assignedEquipment"})
    Optional<TransferJob> findById(Long id);
}
