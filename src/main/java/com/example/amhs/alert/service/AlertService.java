package com.example.amhs.alert.service;

import com.example.amhs.alert.domain.Alert;
import com.example.amhs.alert.domain.AlertStatus;
import com.example.amhs.alert.domain.AlertType;
import com.example.amhs.alert.dto.AlertResponse;
import com.example.amhs.alert.repository.AlertRepository;
import com.example.amhs.common.exception.BusinessException;
import com.example.amhs.common.exception.ErrorCode;
import com.example.amhs.equipment.domain.EquipmentStatus;
import com.example.amhs.transferjob.domain.TransferJob;
import com.example.amhs.transferjob.domain.TransferJobStatus;
import com.example.amhs.transferjob.repository.TransferJobHistoryRepository;
import com.example.amhs.transferjob.repository.TransferJobRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlertService {

    private final AlertRepository alertRepository;
    private final TransferJobRepository transferJobRepository;
    private final TransferJobHistoryRepository transferJobHistoryRepository;

    @Value("${amhs.alert.thresholds.assigned-minutes:5}")
    private long assignedThresholdMinutes;

    @Value("${amhs.alert.thresholds.moving-grace-seconds:0}")
    private long movingGraceSeconds;

    public List<AlertResponse> getAlerts() {
        return alertRepository.findAll()
                .stream()
                .map(AlertResponse::from)
                .toList();
    }

    @Transactional
    public AlertResponse resolveAlert(Long id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ALERT_NOT_FOUND,
                        "Alert not found: " + id
                ));
        alert.resolve();
        return AlertResponse.from(alert);
    }

    @Scheduled(fixedDelayString = "${amhs.alert.scheduler.fixed-delay-ms:60000}")
    @Transactional
    public void detectAlerts() {
        detectStuckAssignedJobs();
        detectDelayedMovingJobs();
        detectEquipmentErrorJobs();
    }

    @Transactional
    public void detectStuckAssignedJobs() {
        LocalDateTime now = LocalDateTime.now();
        for (TransferJob job : transferJobRepository.findByStatus(TransferJobStatus.ASSIGNED)) {
            LocalDateTime baseTime = job.getUpdatedAt() != null ? job.getUpdatedAt() : job.getCreatedAt();
            if (baseTime == null) {
                continue;
            }
            if (Duration.between(baseTime, now).toMinutes() >= assignedThresholdMinutes) {
                createOpenAlertIfAbsent(
                        job,
                        AlertType.STUCK_JOB,
                        "Assigned job is stuck longer than " + assignedThresholdMinutes + " minutes"
                );
            }
        }
    }

    @Transactional
    public void detectDelayedMovingJobs() {
        LocalDateTime now = LocalDateTime.now();
        for (TransferJob job : transferJobRepository.findByStatus(TransferJobStatus.MOVING)) {
            LocalDateTime baseTime = job.getUpdatedAt() != null ? job.getUpdatedAt() : job.getCreatedAt();
            if (baseTime == null) {
                continue;
            }
            long elapsedSeconds = Duration.between(baseTime, now).getSeconds();
            long thresholdSeconds = job.getEstimatedTimeSeconds() + movingGraceSeconds;
            if (elapsedSeconds >= thresholdSeconds) {
                createOpenAlertIfAbsent(
                        job,
                        AlertType.DELAYED_JOB,
                        "Moving job exceeded expected time of " + job.getEstimatedTimeSeconds() + " seconds"
                );
            }
        }
    }

    @Transactional
    public void detectEquipmentErrorJobs() {
        for (TransferJob job : transferJobRepository.findAll()) {
            if (job.getAssignedEquipment() == null) {
                continue;
            }
            if (job.getAssignedEquipment().getStatus() != EquipmentStatus.ERROR) {
                continue;
            }
            if (job.getStatus() == TransferJobStatus.COMPLETED || job.getStatus() == TransferJobStatus.CANCELED) {
                continue;
            }

            job.getAssignedEquipment().changeStatus(EquipmentStatus.IDLE);
            job.updateStatus(TransferJobStatus.FAILED, null, "EQUIPMENT_ERROR");
            createOpenAlertIfAbsent(job, AlertType.EQUIPMENT_ERROR, "Assigned equipment entered ERROR status");

            transferJobHistoryRepository.save(
                    com.example.amhs.transferjob.domain.TransferJobHistory.create(
                            job,
                            TransferJobStatus.FAILED,
                            "EQUIPMENT_ERROR",
                            null,
                            job.getPath()
                    )
            );
        }
    }

    private void createOpenAlertIfAbsent(TransferJob job, AlertType type, String message) {
        if (alertRepository.existsByTransferJobIdAndTypeAndStatus(job.getId(), type, AlertStatus.OPEN)) {
            return;
        }
        alertRepository.save(Alert.open(job, type, message));
    }
}
