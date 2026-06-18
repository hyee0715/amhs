package com.example.amhs.alert.repository;

import com.example.amhs.alert.domain.Alert;
import com.example.amhs.alert.domain.AlertStatus;
import com.example.amhs.alert.domain.AlertType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    @Override
    @EntityGraph(attributePaths = {"transferJob", "transferJob.assignedEquipment"})
    List<Alert> findAll();

    @Override
    @EntityGraph(attributePaths = {"transferJob", "transferJob.assignedEquipment"})
    Optional<Alert> findById(Long id);

    boolean existsByTransferJobIdAndTypeAndStatus(Long transferJobId, AlertType type, AlertStatus status);
}
