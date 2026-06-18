package com.example.amhs.equipment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.amhs.alert.repository.AlertRepository;
import com.example.amhs.common.exception.BusinessException;
import com.example.amhs.common.exception.ErrorCode;
import com.example.amhs.equipment.domain.EquipmentStatus;
import com.example.amhs.equipment.domain.EquipmentType;
import com.example.amhs.equipment.dto.EquipmentCreateRequest;
import com.example.amhs.equipment.dto.EquipmentResponse;
import com.example.amhs.equipment.dto.EquipmentStatusUpdateRequest;
import com.example.amhs.equipment.repository.EquipmentRepository;
import com.example.amhs.transferjob.repository.TransferJobHistoryRepository;
import com.example.amhs.transferjob.repository.TransferJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EquipmentServiceTest {

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private TransferJobHistoryRepository transferJobHistoryRepository;

    @Autowired
    private TransferJobRepository transferJobRepository;

    @Autowired
    private AlertRepository alertRepository;

    @BeforeEach
    void setUp() {
        alertRepository.deleteAll();
        transferJobHistoryRepository.deleteAll();
        transferJobRepository.deleteAll();
        equipmentRepository.deleteAll();
    }

    @Test
    @DisplayName("장비를 등록하면 기본 상태는 IDLE이다")
    void createEquipment() {
        EquipmentResponse response = equipmentService.createEquipment(
                new EquipmentCreateRequest("OHT_001", "OHT 001", EquipmentType.OHT)
        );

        assertThat(response.id()).isNotNull();
        assertThat(response.code()).isEqualTo("OHT_001");
        assertThat(response.status()).isEqualTo(EquipmentStatus.IDLE);
    }

    @Test
    @DisplayName("중복 code로 장비를 등록하면 DUPLICATED_EQUIPMENT_CODE 예외가 발생한다")
    void createEquipmentWithDuplicateCode() {
        equipmentService.createEquipment(
                new EquipmentCreateRequest("OHT_001", "OHT 001", EquipmentType.OHT)
        );

        assertThatThrownBy(() ->
                equipmentService.createEquipment(
                        new EquipmentCreateRequest("OHT_001", "OHT Duplicate", EquipmentType.OHT)
                )
        )
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATED_EQUIPMENT_CODE);
    }

    @Test
    @DisplayName("장비 상태를 변경할 수 있다")
    void updateEquipmentStatus() {
        EquipmentResponse created = equipmentService.createEquipment(
                new EquipmentCreateRequest("ROBOT_001", "Robot 001", EquipmentType.ROBOT)
        );

        EquipmentResponse updated = equipmentService.updateEquipmentStatus(
                created.id(),
                new EquipmentStatusUpdateRequest(EquipmentStatus.ERROR)
        );

        assertThat(updated.status()).isEqualTo(EquipmentStatus.ERROR);
    }

    @Test
    @DisplayName("없는 장비를 조회하면 EQUIPMENT_NOT_FOUND 예외가 발생한다")
    void getEquipmentWithInvalidId() {
        assertThatThrownBy(() -> equipmentService.getEquipment(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.EQUIPMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("장비 목록을 조회할 수 있다")
    void getEquipments() {
        equipmentService.createEquipment(
                new EquipmentCreateRequest("OHT_001", "OHT 001", EquipmentType.OHT)
        );
        equipmentService.createEquipment(
                new EquipmentCreateRequest("CONVEYOR_001", "Conveyor 001", EquipmentType.CONVEYOR)
        );

        assertThat(equipmentService.getEquipments()).hasSize(2);
    }
}
