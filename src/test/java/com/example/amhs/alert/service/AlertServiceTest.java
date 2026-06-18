package com.example.amhs.alert.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.amhs.alert.domain.AlertStatus;
import com.example.amhs.alert.domain.AlertType;
import com.example.amhs.alert.dto.AlertResponse;
import com.example.amhs.alert.repository.AlertRepository;
import com.example.amhs.edge.domain.AmhsEdge;
import com.example.amhs.edge.repository.EdgeRepository;
import com.example.amhs.equipment.domain.Equipment;
import com.example.amhs.equipment.domain.EquipmentStatus;
import com.example.amhs.equipment.domain.EquipmentType;
import com.example.amhs.equipment.repository.EquipmentRepository;
import com.example.amhs.node.domain.AmhsNode;
import com.example.amhs.node.domain.NodeType;
import com.example.amhs.node.repository.NodeRepository;
import com.example.amhs.transferjob.domain.TransferJobPriority;
import com.example.amhs.transferjob.domain.TransferJobStatus;
import com.example.amhs.transferjob.dto.TransferJobCreateRequest;
import com.example.amhs.transferjob.dto.TransferJobStatusUpdateRequest;
import com.example.amhs.transferjob.repository.TransferJobHistoryRepository;
import com.example.amhs.transferjob.repository.TransferJobRepository;
import com.example.amhs.transferjob.service.TransferJobService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class AlertServiceTest {

    @Autowired
    private AlertService alertService;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private TransferJobService transferJobService;

    @Autowired
    private TransferJobHistoryRepository transferJobHistoryRepository;

    @Autowired
    private TransferJobRepository transferJobRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private EdgeRepository edgeRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        alertRepository.deleteAll();
        transferJobHistoryRepository.deleteAll();
        transferJobRepository.deleteAll();
        edgeRepository.deleteAll();
        equipmentRepository.deleteAll();
        nodeRepository.deleteAll();

        ReflectionTestUtils.setField(alertService, "assignedThresholdMinutes", 5L);
        ReflectionTestUtils.setField(alertService, "movingGraceSeconds", 0L);

        AmhsNode stocker = nodeRepository.save(AmhsNode.create("STOCKER_01", "Stocker 01", NodeType.STOCKER));
        AmhsNode nodeA = nodeRepository.save(AmhsNode.create("NODE_A", "Node A", NodeType.OHT_NODE));
        AmhsNode eqp = nodeRepository.save(AmhsNode.create("EQP_01", "Equipment 01", NodeType.EQP));

        edgeRepository.save(AmhsEdge.create(stocker, nodeA, 100, 10));
        edgeRepository.save(AmhsEdge.create(nodeA, eqp, 100, 10));

        equipmentRepository.save(Equipment.create("OHT_001", "OHT 001", EquipmentType.OHT));
    }

    @Test
    @DisplayName("ASSIGNED 상태가 오래 지속되면 STUCK_JOB 알림이 생성된다")
    void detectStuckAssignedJobs() {
        var created = transferJobService.createTransferJob(
                new TransferJobCreateRequest("FOUP-001", "STOCKER_01", "EQP_01", TransferJobPriority.NORMAL)
        );
        transferJobService.assignTransferJob(created.id());

        jdbcTemplate.update(
                "update transfer_jobs set updated_at = ? where id = ?",
                LocalDateTime.now().minusMinutes(10),
                created.id()
        );

        alertService.detectStuckAssignedJobs();

        List<AlertResponse> alerts = alertService.getAlerts();
        assertThat(alerts).hasSize(1);
        assertThat(alerts.getFirst().type()).isEqualTo(AlertType.STUCK_JOB);
        assertThat(alerts.getFirst().status()).isEqualTo(AlertStatus.OPEN);
    }

    @Test
    @DisplayName("MOVING 상태가 예상 시간을 초과하면 DELAYED_JOB 알림이 생성된다")
    void detectDelayedMovingJobs() {
        var created = transferJobService.createTransferJob(
                new TransferJobCreateRequest("FOUP-001", "STOCKER_01", "EQP_01", TransferJobPriority.NORMAL)
        );
        transferJobService.assignTransferJob(created.id());
        transferJobService.updateTransferJobStatus(
                created.id(),
                new TransferJobStatusUpdateRequest(TransferJobStatus.MOVING, "Start move", null)
        );

        jdbcTemplate.update(
                "update transfer_jobs set updated_at = ? where id = ?",
                LocalDateTime.now().minusSeconds(20),
                created.id()
        );

        alertService.detectDelayedMovingJobs();

        List<AlertResponse> alerts = alertService.getAlerts();
        assertThat(alerts).hasSize(1);
        assertThat(alerts.getFirst().type()).isEqualTo(AlertType.DELAYED_JOB);
    }

    @Test
    @DisplayName("ERROR 장비에 할당된 Job은 FAILED 처리되고 EQUIPMENT_ERROR 알림이 생성된다")
    void detectEquipmentErrorJobs() {
        var created = transferJobService.createTransferJob(
                new TransferJobCreateRequest("FOUP-001", "STOCKER_01", "EQP_01", TransferJobPriority.NORMAL)
        );
        transferJobService.assignTransferJob(created.id());

        Equipment equipment = equipmentRepository.findByCode("OHT_001").orElseThrow();
        equipment.changeStatus(EquipmentStatus.ERROR);
        equipmentRepository.save(equipment);

        alertService.detectEquipmentErrorJobs();

        var job = transferJobRepository.findById(created.id()).orElseThrow();
        List<AlertResponse> alerts = alertService.getAlerts();

        assertThat(job.getStatus()).isEqualTo(TransferJobStatus.FAILED);
        assertThat(job.getFailureReason()).isEqualTo("EQUIPMENT_ERROR");
        assertThat(alerts).hasSize(1);
        assertThat(alerts.getFirst().type()).isEqualTo(AlertType.EQUIPMENT_ERROR);
    }

    @Test
    @DisplayName("알림을 resolve 할 수 있다")
    void resolveAlert() {
        var created = transferJobService.createTransferJob(
                new TransferJobCreateRequest("FOUP-001", "STOCKER_01", "EQP_01", TransferJobPriority.NORMAL)
        );
        transferJobService.assignTransferJob(created.id());

        jdbcTemplate.update(
                "update transfer_jobs set updated_at = ? where id = ?",
                LocalDateTime.now().minusMinutes(10),
                created.id()
        );
        alertService.detectStuckAssignedJobs();

        Long alertId = alertRepository.findAll().getFirst().getId();
        AlertResponse resolved = alertService.resolveAlert(alertId);

        assertThat(resolved.status()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(resolved.resolvedAt()).isNotNull();
    }
}
