package com.example.amhs.transferjob.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(name = "transfer_job_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class TransferJobHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transfer_job_id", nullable = false)
    private TransferJob transferJob;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransferJobStatus status;

    @Column(length = 500)
    private String reason;

    @Column(length = 100)
    private String assignedEquipmentCode;

    @Column(columnDefinition = "TEXT")
    private String pathSnapshot;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private TransferJobHistory(
            TransferJob transferJob,
            TransferJobStatus status,
            String reason,
            String assignedEquipmentCode,
            String pathSnapshot
    ) {
        this.transferJob = transferJob;
        this.status = status;
        this.reason = reason;
        this.assignedEquipmentCode = assignedEquipmentCode;
        this.pathSnapshot = pathSnapshot;
    }

    public static TransferJobHistory create(
            TransferJob transferJob,
            TransferJobStatus status,
            String reason,
            String assignedEquipmentCode,
            String pathSnapshot
    ) {
        return TransferJobHistory.builder()
                .transferJob(transferJob)
                .status(status)
                .reason(reason)
                .assignedEquipmentCode(assignedEquipmentCode)
                .pathSnapshot(pathSnapshot)
                .build();
    }
}
