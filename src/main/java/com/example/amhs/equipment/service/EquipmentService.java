package com.example.amhs.equipment.service;

import com.example.amhs.common.exception.BusinessException;
import com.example.amhs.common.exception.ErrorCode;
import com.example.amhs.equipment.domain.Equipment;
import com.example.amhs.equipment.dto.EquipmentCreateRequest;
import com.example.amhs.equipment.dto.EquipmentResponse;
import com.example.amhs.equipment.dto.EquipmentStatusUpdateRequest;
import com.example.amhs.equipment.repository.EquipmentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;

    @Transactional
    public EquipmentResponse createEquipment(EquipmentCreateRequest request) {
        validateDuplicateCode(request.code());

        Equipment equipment = Equipment.create(request.code(), request.name(), request.type());
        return EquipmentResponse.from(equipmentRepository.save(equipment));
    }

    public List<EquipmentResponse> getEquipments() {
        return equipmentRepository.findAll()
                .stream()
                .map(EquipmentResponse::from)
                .toList();
    }

    public EquipmentResponse getEquipment(Long id) {
        return EquipmentResponse.from(findEquipmentById(id));
    }

    @Transactional
    public EquipmentResponse updateEquipmentStatus(Long id, EquipmentStatusUpdateRequest request) {
        Equipment equipment = findEquipmentById(id);
        equipment.changeStatus(request.status());
        return EquipmentResponse.from(equipment);
    }

    private void validateDuplicateCode(String code) {
        if (equipmentRepository.existsByCode(code)) {
            throw new BusinessException(
                    ErrorCode.DUPLICATED_EQUIPMENT_CODE,
                    "Duplicated equipment code: " + code
            );
        }
    }

    private Equipment findEquipmentById(Long id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.EQUIPMENT_NOT_FOUND,
                        "Equipment not found: " + id
                ));
    }
}
