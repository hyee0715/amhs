package com.example.amhs.analytics.dto;

public record FailureParetoResponse(
        String failureReason,
        Long count,
        Double ratio,
        Double cumulativeRatio
) {
}
