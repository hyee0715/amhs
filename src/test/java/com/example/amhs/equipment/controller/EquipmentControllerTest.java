package com.example.amhs.equipment.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.amhs.equipment.repository.EquipmentRepository;
import com.example.amhs.transferjob.repository.TransferJobHistoryRepository;
import com.example.amhs.transferjob.repository.TransferJobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class EquipmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private TransferJobHistoryRepository transferJobHistoryRepository;

    @Autowired
    private TransferJobRepository transferJobRepository;

    @BeforeEach
    void setUp() {
        transferJobHistoryRepository.deleteAll();
        transferJobRepository.deleteAll();
        equipmentRepository.deleteAll();
    }

    @Test
    @DisplayName("장비를 등록할 수 있다")
    void createEquipment() throws Exception {
        mockMvc.perform(post("/api/equipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "OHT_001",
                                  "name": "OHT 001",
                                  "type": "OHT"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("OHT_001"))
                .andExpect(jsonPath("$.status").value("IDLE"));
    }

    @Test
    @DisplayName("중복 code로 장비를 등록하면 예외가 발생한다")
    void createEquipmentWithDuplicateCode() throws Exception {
        String request = """
                {
                  "code": "OHT_001",
                  "name": "OHT 001",
                  "type": "OHT"
                }
                """;

        mockMvc.perform(post("/api/equipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/equipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATED_EQUIPMENT_CODE"));
    }

    @Test
    @DisplayName("장비 상태를 변경할 수 있다")
    void updateEquipmentStatus() throws Exception {
        String response = mockMvc.perform(post("/api/equipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "ROBOT_001",
                                  "name": "Robot 001",
                                  "type": "ROBOT"
                                }
                                """))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(response).path("id").asLong();

        mockMvc.perform(patch("/api/equipments/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "ERROR"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    @Test
    @DisplayName("장비 단건 조회가 가능하다")
    void getEquipment() throws Exception {
        String response = mockMvc.perform(post("/api/equipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "CONVEYOR_001",
                                  "name": "Conveyor 001",
                                  "type": "CONVEYOR"
                                }
                                """))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(response).path("id").asLong();

        mockMvc.perform(get("/api/equipments/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CONVEYOR_001"));
    }

    @Test
    @DisplayName("장비 목록 조회가 가능하다")
    void getEquipments() throws Exception {
        mockMvc.perform(post("/api/equipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "OHT_001",
                                  "name": "OHT 001",
                                  "type": "OHT"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/equipments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("OHT_001"));
    }

    @Test
    @DisplayName("없는 장비를 조회하면 예외가 발생한다")
    void getEquipmentWithInvalidId() throws Exception {
        mockMvc.perform(get("/api/equipments/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("EQUIPMENT_NOT_FOUND"));
    }
}
