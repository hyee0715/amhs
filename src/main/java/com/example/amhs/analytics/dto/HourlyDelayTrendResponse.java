package com.example.amhs.analytics.dto;

public record HourlyDelayTrendResponse(
        Integer hour,
        Long totalCompletedJobs,
        Long delayedJobs,
        Double delayRate,
        Double movingAverageDelayRate
) {
}
