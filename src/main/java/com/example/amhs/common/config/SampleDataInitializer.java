package com.example.amhs.common.config;

import com.example.amhs.edge.domain.AmhsEdge;
import com.example.amhs.edge.repository.EdgeRepository;
import com.example.amhs.equipment.domain.Equipment;
import com.example.amhs.equipment.repository.EquipmentRepository;
import com.example.amhs.node.domain.AmhsNode;
import com.example.amhs.node.domain.NodeType;
import com.example.amhs.node.repository.NodeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local")
@RequiredArgsConstructor
public class SampleDataInitializer implements ApplicationRunner {

    private final NodeRepository nodeRepository;
    private final EdgeRepository edgeRepository;
    private final EquipmentRepository equipmentRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (hasAnyData()) {
            return;
        }

        initializeNodesAndEdges();
        initializeEquipments();
    }

    private boolean hasAnyData() {
        return nodeRepository.count() > 0 || edgeRepository.count() > 0 || equipmentRepository.count() > 0;
    }

    private void initializeNodesAndEdges() {
        AmhsNode stocker01 = nodeRepository.save(AmhsNode.create("STOCKER_01", "Stocker 01", NodeType.STOCKER));
        AmhsNode nodeA = nodeRepository.save(AmhsNode.create("NODE_A", "Node A", NodeType.OHT_NODE));
        AmhsNode nodeB = nodeRepository.save(AmhsNode.create("NODE_B", "Node B", NodeType.OHT_NODE));
        AmhsNode nodeC = nodeRepository.save(AmhsNode.create("NODE_C", "Node C", NodeType.OHT_NODE));
        AmhsNode nodeD = nodeRepository.save(AmhsNode.create("NODE_D", "Node D", NodeType.OHT_NODE));
        AmhsNode eqp01 = nodeRepository.save(AmhsNode.create("EQP_01", "Equipment 01", NodeType.EQP));
        AmhsNode eqp02 = nodeRepository.save(AmhsNode.create("EQP_02", "Equipment 02", NodeType.EQP));

        edgeRepository.saveAll(List.of(
                AmhsEdge.create(stocker01, nodeA, 100, 30),
                AmhsEdge.create(nodeA, nodeB, 120, 40),
                AmhsEdge.create(nodeB, eqp01, 80, 30),
                AmhsEdge.create(stocker01, nodeC, 150, 50),
                AmhsEdge.create(nodeC, nodeD, 70, 20),
                AmhsEdge.create(nodeD, eqp02, 90, 30),
                AmhsEdge.create(nodeA, nodeD, 85, 25),
                AmhsEdge.create(nodeA, stocker01, 100, 30),
                AmhsEdge.create(nodeB, nodeA, 120, 40),
                AmhsEdge.create(eqp01, nodeB, 80, 30),
                AmhsEdge.create(nodeC, stocker01, 150, 50),
                AmhsEdge.create(nodeD, nodeC, 70, 20),
                AmhsEdge.create(eqp02, nodeD, 90, 30),
                AmhsEdge.create(nodeD, nodeA, 85, 25)
        ));
    }

    private void initializeEquipments() {
        equipmentRepository.saveAll(List.of(
                Equipment.create("OHT_001", "OHT 001", com.example.amhs.equipment.domain.EquipmentType.OHT),
                Equipment.create("OHT_002", "OHT 002", com.example.amhs.equipment.domain.EquipmentType.OHT),
                Equipment.create("CONVEYOR_001", "Conveyor 001", com.example.amhs.equipment.domain.EquipmentType.CONVEYOR)
        ));
    }
}
