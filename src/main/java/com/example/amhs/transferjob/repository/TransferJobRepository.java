package com.example.amhs.transferjob.repository;

import com.example.amhs.transferjob.domain.TransferJob;
import com.example.amhs.transferjob.domain.TransferJobStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TransferJobRepository extends JpaRepository<TransferJob, Long> {

    @Override
    @EntityGraph(attributePaths = {"sourceNode", "destinationNode", "assignedEquipment"})
    List<TransferJob> findAll();

    @Override
    @EntityGraph(attributePaths = {"sourceNode", "destinationNode", "assignedEquipment"})
    Optional<TransferJob> findById(Long id);

    long countByStatus(TransferJobStatus status);

    @Query("select coalesce(avg(t.estimatedTimeSeconds), 0) from TransferJob t")
    Double findAverageEstimatedTimeSeconds();
}
