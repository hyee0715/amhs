package com.example.amhs.equipment.repository;

import com.example.amhs.equipment.domain.Equipment;
import com.example.amhs.equipment.domain.EquipmentStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    boolean existsByCode(String code);

    Optional<Equipment> findByCode(String code);

    long countByStatus(EquipmentStatus status);
}
