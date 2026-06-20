package com.example.amhs.analytics.dto;

import java.util.List;

public record RouteStabilityResponse(
        List<String> route,
        Long jobCount,
        Double averageTransferTimeSeconds,
        Double standardDeviation,
        Double coefficientOfVariation,
        Integer minTransferTimeSeconds,
        Integer maxTransferTimeSeconds,
        RouteStabilityLevel stability
) {
}
