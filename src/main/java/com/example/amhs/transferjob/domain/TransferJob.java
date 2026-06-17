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

    private LocalDateTime completedAt;

    private LocalDateTime failedAt;

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
            LocalDateTime completedAt,
            LocalDateTime failedAt
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
        this.completedAt = completedAt;
        this.failedAt = failedAt;
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

        if (status == TransferJobStatus.COMPLETED) {
            this.failureReason = null;
            this.completedAt = LocalDateTime.now();
            this.failedAt = null;
        } else if (status == TransferJobStatus.FAILED) {
            this.failureReason = failureReason;
            this.completedAt = null;
            this.failedAt = LocalDateTime.now();
        } else {
            this.failedAt = null;
        }
    }

    public void retry(String path, int estimatedTimeSeconds) {
        this.status = TransferJobStatus.CREATED;
        this.path = path;
        this.estimatedTimeSeconds = estimatedTimeSeconds;
        this.retryCount += 1;
        this.assignedEquipment = null;
        this.failureReason = null;
        this.completedAt = null;
        this.failedAt = null;
    }
}
