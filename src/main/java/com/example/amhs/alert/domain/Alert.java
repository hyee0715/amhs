package com.example.amhs.alert.domain;

import com.example.amhs.common.domain.BaseTimeEntity;
import com.example.amhs.transferjob.domain.TransferJob;
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
@Table(name = "alerts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Alert extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_job_id")
    private TransferJob transferJob;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AlertType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AlertStatus status;

    @Column(nullable = false, length = 500)
    private String message;

    private LocalDateTime resolvedAt;

    @Builder
    private Alert(
            TransferJob transferJob,
            AlertType type,
            AlertStatus status,
            String message,
            LocalDateTime resolvedAt
    ) {
        this.transferJob = transferJob;
        this.type = type;
        this.status = status;
        this.message = message;
        this.resolvedAt = resolvedAt;
    }

    public static Alert open(TransferJob transferJob, AlertType type, String message) {
        return Alert.builder()
                .transferJob(transferJob)
                .type(type)
                .status(AlertStatus.OPEN)
                .message(message)
                .build();
    }

    public void resolve() {
        this.status = AlertStatus.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
    }
}
