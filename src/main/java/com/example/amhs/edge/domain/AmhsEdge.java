package com.example.amhs.edge.domain;

import com.example.amhs.common.domain.BaseTimeEntity;
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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "amhs_edges")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AmhsEdge extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_node_id", nullable = false)
    private AmhsNode fromNode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_node_id", nullable = false)
    private AmhsNode toNode;

    @Column(nullable = false)
    private int distance;

    @Column(nullable = false)
    private int estimatedTimeSeconds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EdgeStatus status;

    @Builder
    private AmhsEdge(
            AmhsNode fromNode,
            AmhsNode toNode,
            int distance,
            int estimatedTimeSeconds,
            EdgeStatus status
    ) {
        this.fromNode = fromNode;
        this.toNode = toNode;
        this.distance = distance;
        this.estimatedTimeSeconds = estimatedTimeSeconds;
        this.status = status;
    }

    public static AmhsEdge create(
            AmhsNode fromNode,
            AmhsNode toNode,
            int distance,
            int estimatedTimeSeconds
    ) {
        return AmhsEdge.builder()
                .fromNode(fromNode)
                .toNode(toNode)
                .distance(distance)
                .estimatedTimeSeconds(estimatedTimeSeconds)
                .status(EdgeStatus.AVAILABLE)
                .build();
    }

    public void changeStatus(EdgeStatus status) {
        this.status = status;
    }
}
