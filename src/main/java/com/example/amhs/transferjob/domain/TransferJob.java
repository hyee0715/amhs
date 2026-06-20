package com.example.amhs.transferjob.domain;

import com.example.amhs.common.domain.BaseTimeEntity;
import com.example.amhs.equipment.domain.Equipment;
import com.example.amhs.node.domain.AmhsNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "transfer_jobs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransferJob extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String carrierId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_node_id", nullable = false)
    private AmhsNode sourceNode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_node_id", nullable = false)
    private AmhsNode destinationNode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_equipment_id")
    private Equipment assignedEquipment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransferJobStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransferJobPriority priority;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String path;

    @Column(nullable = false)
    private int estimatedTimeSeconds;

    @Column(nullable = false)
    private int retryCount;

    @Column(length = 500)
    private String failureReason;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private LocalDateTime failedAt;

    private Integer actualTransferTimeSeconds;

    @Builder
    private TransferJob(
            String carrierId,
            AmhsNode sourceNode,
            AmhsNode destinationNode,
            Equipment assignedEquipment,
            TransferJobStatus status,
            TransferJobPriority priority,
            String path,
            int estimatedTimeSeconds,
            int retryCount,
            String failureReason,
            LocalDateTime startedAt,
            LocalDateTime completedAt,
            LocalDateTime failedAt,
            Integer actualTransferTimeSeconds
    ) {
        this.carrierId = carrierId;
        this.sourceNode = sourceNode;
        this.destinationNode = destinationNode;
        this.assignedEquipment = assignedEquipment;
        this.status = status;
        this.priority = priority;
        this.path = path;
        this.estimatedTimeSeconds = estimatedTimeSeconds;
        this.retryCount = retryCount;
        this.failureReason = failureReason;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.failedAt = failedAt;
        this.actualTransferTimeSeconds = actualTransferTimeSeconds;
    }

    public static TransferJob create(
            String carrierId,
            AmhsNode sourceNode,
            AmhsNode destinationNode,
            TransferJobPriority priority,
            String path,
            int estimatedTimeSeconds
    ) {
        return TransferJob.builder()
                .carrierId(carrierId)
                .sourceNode(sourceNode)
                .destinationNode(destinationNode)
                .status(TransferJobStatus.CREATED)
                .priority(priority)
                .path(path)
                .estimatedTimeSeconds(estimatedTimeSeconds)
                .retryCount(0)
                .build();
    }

    public void updateStatus(TransferJobStatus status, Equipment assignedEquipment, String failureReason) {
        this.status = status;
        if (assignedEquipment != null) {
            this.assignedEquipment = assignedEquipment;
        }

        LocalDateTime now = LocalDateTime.now();
        if (status == TransferJobStatus.MOVING) {
            this.startedAt = now;
            this.failureReason = null;
            this.completedAt = null;
            this.failedAt = null;
            this.actualTransferTimeSeconds = null;
            return;
        }

        if (status == TransferJobStatus.COMPLETED) {
            this.failureReason = null;
            this.completedAt = now;
            this.failedAt = null;
            this.actualTransferTimeSeconds = startedAt != null
                    ? Math.toIntExact(Duration.between(startedAt, completedAt).getSeconds())
                    : null;
            return;
        }

        if (status == TransferJobStatus.FAILED) {
            this.failureReason = failureReason;
            this.completedAt = null;
            this.failedAt = now;
            this.actualTransferTimeSeconds = null;
            return;
        }

        this.failedAt = null;
        this.completedAt = null;
        this.actualTransferTimeSeconds = null;
    }

    public void retry(String path, int estimatedTimeSeconds) {
        this.status = TransferJobStatus.CREATED;
        this.path = path;
        this.estimatedTimeSeconds = estimatedTimeSeconds;
        this.retryCount += 1;
        this.assignedEquipment = null;
        this.failureReason = null;
        this.startedAt = null;
        this.completedAt = null;
        this.failedAt = null;
        this.actualTransferTimeSeconds = null;
    }

    public void assign(Equipment equipment) {
        this.status = TransferJobStatus.ASSIGNED;
        this.assignedEquipment = equipment;
        this.failureReason = null;
    }

    public void overrideCompletionMetrics(LocalDateTime startedAt, LocalDateTime completedAt, int actualTransferTimeSeconds) {
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.actualTransferTimeSeconds = actualTransferTimeSeconds;
        this.failedAt = null;
        this.status = TransferJobStatus.COMPLETED;
    }

    public void clearAssignedEquipment() {
        this.assignedEquipment = null;
    }
}
