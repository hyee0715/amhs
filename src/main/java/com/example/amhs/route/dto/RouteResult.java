package com.example.amhs.route.dto;

import java.util.List;

public record RouteResult(
        String source,
        String destination,
        String algorithm,
        List<String> path,
        int totalEstimatedTimeSeconds,
        int totalDistance
) {
    public static RouteResult of(
            String source,
            String destination,
            String algorithm,
            List<String> path,
            int totalEstimatedTimeSeconds,
            int totalDistance
    ) {
        return new RouteResult(source, destination, algorithm, path, totalEstimatedTimeSeconds, totalDistance);
    }
}
