package com.example.amhs.dashboard.dto;

public record DashboardSummaryResponse(
        long totalJobs,
        long createdJobs,
        long movingJobs,
        long completedJobs,
        long failedJobs,
        double successRate,
        double averageEstimatedTimeSeconds,
        long blockedNodeCount,
        long blockedEdgeCount,
        long equipmentErrorCount
) {
}
