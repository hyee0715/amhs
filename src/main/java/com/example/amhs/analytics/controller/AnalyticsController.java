package com.example.amhs.analytics.controller;

import com.example.amhs.analytics.dto.TransferTimeOutlierResponse;
import com.example.amhs.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analytics")
@Tag(name = "Analytics", description = "AMHS 운영 데이터 분석 API")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/transfer-time/outliers")
    @Operation(summary = "IQR 기반 이상 지연 Job 탐지")
    public ResponseEntity<TransferTimeOutlierResponse> getTransferTimeOutliers() {
        return ResponseEntity.ok(analyticsService.getTransferTimeOutliers());
    }
}
