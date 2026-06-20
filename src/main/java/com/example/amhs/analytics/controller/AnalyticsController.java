package com.example.amhs.analytics.controller;

import com.example.amhs.analytics.dto.FailureParetoResponse;
import com.example.amhs.analytics.dto.HourlyDelayTrendResponse;
import com.example.amhs.analytics.dto.RouteStabilityResponse;
import com.example.amhs.analytics.dto.TransferTimeOutlierResponse;
import com.example.amhs.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
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

    @GetMapping("/routes/stability")
    @Operation(summary = "경로별 평균/표준편차/변동계수 기반 불안정 경로 탐지")
    public ResponseEntity<List<RouteStabilityResponse>> getRouteStabilities() {
        return ResponseEntity.ok(analyticsService.getRouteStabilities());
    }

    @GetMapping("/failures/pareto")
    @Operation(summary = "실패 원인 파레토 분석")
    public ResponseEntity<List<FailureParetoResponse>> getFailurePareto() {
        return ResponseEntity.ok(analyticsService.getFailurePareto());
    }

    @GetMapping("/delays/hourly-trend")
    @Operation(summary = "시간대별 지연률 및 3시간 이동평균 추세 분석")
    public ResponseEntity<List<HourlyDelayTrendResponse>> getHourlyDelayTrends() {
        return ResponseEntity.ok(analyticsService.getHourlyDelayTrends());
    }
}
