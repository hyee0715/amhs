package com.example.amhs.node.domain;

import com.example.amhs.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "amhs_nodes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AmhsNode extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NodeType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NodeStatus status;

    @Builder
    private AmhsNode(String code, String name, NodeType type, NodeStatus status) {
        this.code = code;
        this.name = name;
        this.type = type;
        this.status = status;
    }

    public static AmhsNode create(String code, String name, NodeType type) {
        return AmhsNode.builder()
                .code(code)
                .name(name)
                .type(type)
                .status(NodeStatus.AVAILABLE)
                .build();
    }

    public void changeStatus(NodeStatus status) {
        this.status = status;
    }
}
