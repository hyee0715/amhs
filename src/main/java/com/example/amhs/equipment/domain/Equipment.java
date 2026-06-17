package com.example.amhs.equipment.domain;

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
@Table(name = "equipments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Equipment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EquipmentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EquipmentStatus status;

    @Builder
    private Equipment(String code, String name, EquipmentType type, EquipmentStatus status) {
        this.code = code;
        this.name = name;
        this.type = type;
        this.status = status;
    }

    public static Equipment create(String code, String name, EquipmentType type) {
        return Equipment.builder()
                .code(code)
                .name(name)
                .type(type)
                .status(EquipmentStatus.IDLE)
                .build();
    }

    public void changeStatus(EquipmentStatus status) {
        this.status = status;
    }
}
