package com.example.amhs.analytics.service;

import com.example.amhs.analytics.dto.FailureParetoResponse;
import com.example.amhs.analytics.dto.HourlyDelayTrendResponse;
import com.example.amhs.analytics.dto.RouteStabilityLevel;
import com.example.amhs.analytics.dto.RouteStabilityResponse;
import com.example.amhs.analytics.dto.TransferTimeOutlierJobResponse;
import com.example.amhs.analytics.dto.TransferTimeOutlierResponse;
import com.example.amhs.transferjob.domain.TransferJob;
import com.example.amhs.transferjob.domain.TransferJobStatus;
import com.example.amhs.transferjob.repository.TransferJobRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final TransferJobRepository transferJobRepository;
    private final ObjectMapper objectMapper;

    public TransferTimeOutlierResponse getTransferTimeOutliers() {
        List<TransferJob> completedJobs = transferJobRepository.findByStatusAndActualTransferTimeSecondsIsNotNull(
                TransferJobStatus.COMPLETED
        );

        long jobCount = completedJobs.size();
        if (jobCount < 4) {
            return TransferTimeOutlierResponse.insufficientData(jobCount);
        }

        List<Integer> sortedTransferTimes = completedJobs.stream()
                .map(TransferJob::getActualTransferTimeSeconds)
                .sorted()
                .toList();

        double q1 = roundToTwoDecimals(percentile(sortedTransferTimes, 0.25));
        double q3 = roundToTwoDecimals(percentile(sortedTransferTimes, 0.75));
        double iqr = roundToTwoDecimals(q3 - q1);
        double outlierThreshold = roundToTwoDecimals(q3 + (1.5 * iqr));

        List<TransferTimeOutlierJobResponse> outlierJobs = completedJobs.stream()
                .filter(job -> job.getActualTransferTimeSeconds() > outlierThreshold)
                .sorted(Comparator.comparing(TransferJob::getActualTransferTimeSeconds).reversed())
                .map(this::toTransferTimeOutlierJobResponse)
                .toList();

        return new TransferTimeOutlierResponse(
                jobCount,
                q1,
                q3,
                iqr,
                outlierThreshold,
                (long) outlierJobs.size(),
                outlierJobs
        );
    }

    public List<RouteStabilityResponse> getRouteStabilities() {
        List<TransferJob> completedJobs = transferJobRepository.findByStatusAndActualTransferTimeSecondsIsNotNull(
                TransferJobStatus.COMPLETED
        ).stream()
                .filter(job -> job.getPath() != null)
                .toList();

        Map<String, List<TransferJob>> jobsByRoute = completedJobs.stream()
                .collect(Collectors.groupingBy(TransferJob::getPath));

        return jobsByRoute.entrySet().stream()
                .map(entry -> toRouteStabilityResponse(entry.getKey(), entry.getValue()))
                .sorted(Comparator
                        .comparing((RouteStabilityResponse response) -> stabilityRank(response.stability()))
                .thenComparing(RouteStabilityResponse::coefficientOfVariation, Comparator.reverseOrder()))
                .toList();
    }

    public List<FailureParetoResponse> getFailurePareto() {
        List<TransferJob> failedJobs = transferJobRepository.findByStatus(TransferJobStatus.FAILED).stream()
                .filter(job -> job.getFailureReason() != null && !job.getFailureReason().isBlank())
                .toList();

        if (failedJobs.isEmpty()) {
            return List.of();
        }

        long totalFailureCount = failedJobs.size();
        Map<String, Long> countsByFailureReason = failedJobs.stream()
                .collect(Collectors.groupingBy(TransferJob::getFailureReason, Collectors.counting()));

        List<Map.Entry<String, Long>> sortedEntries = countsByFailureReason.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .toList();

        double cumulativeRatio = 0.0;
        List<FailureParetoResponse> responses = new ArrayList<>();
        for (Map.Entry<String, Long> entry : sortedEntries) {
            double ratio = roundToTwoDecimals(entry.getValue() * 100.0 / totalFailureCount);
            cumulativeRatio = roundToTwoDecimals(cumulativeRatio + ratio);
            responses.add(new FailureParetoResponse(
                    entry.getKey(),
                    entry.getValue(),
                    ratio,
                    cumulativeRatio
            ));
        }

        return responses;
    }

    public List<HourlyDelayTrendResponse> getHourlyDelayTrends() {
        List<TransferJob> completedJobs = transferJobRepository.findByStatusAndActualTransferTimeSecondsIsNotNull(
                TransferJobStatus.COMPLETED
        ).stream()
                .filter(job -> job.getCompletedAt() != null)
                .toList();

        Map<Integer, List<TransferJob>> jobsByHour = completedJobs.stream()
                .collect(Collectors.groupingBy(job -> job.getCompletedAt().getHour()));

        List<HourlyDelayTrendResponse> responses = jobsByHour.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    long totalCompletedJobs = entry.getValue().size();
                    long delayedJobs = entry.getValue().stream()
                            .filter(job -> job.getActualTransferTimeSeconds() > job.getEstimatedTimeSeconds())
                            .count();
                    double delayRate = roundToTwoDecimals(delayedJobs * 100.0 / totalCompletedJobs);
                    return new HourlyDelayTrendResponse(
                            entry.getKey(),
                            totalCompletedJobs,
                            delayedJobs,
                            delayRate,
                            0.0
                    );
                })
                .collect(Collectors.toCollection(ArrayList::new));

        for (int i = 0; i < responses.size(); i++) {
            int startIndex = Math.max(0, i - 2);
            double movingAverageDelayRate = roundToTwoDecimals(
                    responses.subList(startIndex, i + 1).stream()
                            .mapToDouble(HourlyDelayTrendResponse::delayRate)
                            .average()
                            .orElse(0.0)
            );

            HourlyDelayTrendResponse current = responses.get(i);
            responses.set(i, new HourlyDelayTrendResponse(
                    current.hour(),
                    current.totalCompletedJobs(),
                    current.delayedJobs(),
                    current.delayRate(),
                    movingAverageDelayRate
            ));
        }

        return responses;
    }

    private TransferTimeOutlierJobResponse toTransferTimeOutlierJobResponse(TransferJob job) {
        return new TransferTimeOutlierJobResponse(
                job.getId(),
                job.getCarrierId(),
                job.getSourceNode().getCode(),
                job.getDestinationNode().getCode(),
                job.getActualTransferTimeSeconds(),
                fromJson(job.getPath()),
                job.getCompletedAt()
        );
    }

    private RouteStabilityResponse toRouteStabilityResponse(String routeJson, List<TransferJob> jobs) {
        List<Integer> transferTimes = jobs.stream()
                .map(TransferJob::getActualTransferTimeSeconds)
                .toList();

        long jobCount = transferTimes.size();
        double average = roundToTwoDecimals(calculateAverage(transferTimes));
        double standardDeviation = jobCount == 1
                ? 0.0
                : roundToTwoDecimals(calculatePopulationStandardDeviation(transferTimes, average));
        double coefficientOfVariation = average == 0.0
                ? 0.0
                : roundToTwoDecimals(standardDeviation / average);

        return new RouteStabilityResponse(
                fromJson(routeJson),
                jobCount,
                average,
                standardDeviation,
                coefficientOfVariation,
                transferTimes.stream().min(Integer::compareTo).orElse(0),
                transferTimes.stream().max(Integer::compareTo).orElse(0),
                classifyStability(jobCount, coefficientOfVariation)
        );
    }

    /**
     * Percentile is computed with linear interpolation on the zero-based position `(n - 1) * p`.
     * This keeps quartile calculation stable for both odd and even sample sizes.
     */
    private double percentile(List<Integer> sortedValues, double percentile) {
        double position = (sortedValues.size() - 1) * percentile;
        int lowerIndex = (int) Math.floor(position);
        int upperIndex = (int) Math.ceil(position);

        if (lowerIndex == upperIndex) {
            return sortedValues.get(lowerIndex);
        }

        double weight = position - lowerIndex;
        return sortedValues.get(lowerIndex)
                + (sortedValues.get(upperIndex) - sortedValues.get(lowerIndex)) * weight;
    }

    private List<String> fromJson(String path) {
        if (path == null || path.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(path, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize path", exception);
        }
    }

    private double calculateAverage(List<Integer> values) {
        return values.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
    }

    private double calculatePopulationStandardDeviation(List<Integer> values, double average) {
        double variance = values.stream()
                .mapToDouble(value -> Math.pow(value - average, 2))
                .average()
                .orElse(0.0);
        return Math.sqrt(variance);
    }

    private RouteStabilityLevel classifyStability(long jobCount, double coefficientOfVariation) {
        if (jobCount == 1 || coefficientOfVariation < 0.15) {
            return RouteStabilityLevel.STABLE;
        }
        if (coefficientOfVariation < 0.35) {
            return RouteStabilityLevel.MODERATE;
        }
        return RouteStabilityLevel.UNSTABLE;
    }

    private int stabilityRank(RouteStabilityLevel stability) {
        return switch (stability) {
            case UNSTABLE -> 0;
            case MODERATE -> 1;
            case STABLE -> 2;
        };
    }

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
