package com.example.amhs.alert.controller;

import com.example.amhs.alert.dto.AlertResponse;
import com.example.amhs.alert.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/alerts")
@Tag(name = "Alert", description = "지연 및 장애 알림 API")
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    @Operation(summary = "알림 목록 조회")
    public ResponseEntity<List<AlertResponse>> getAlerts() {
        return ResponseEntity.ok(alertService.getAlerts());
    }

    @PatchMapping("/{id}/resolve")
    @Operation(summary = "알림 해제")
    public ResponseEntity<AlertResponse> resolveAlert(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.resolveAlert(id));
    }
}
