package com.example.amhs.analytics.service;

import com.example.amhs.analytics.dto.TransferTimeOutlierJobResponse;
import com.example.amhs.analytics.dto.TransferTimeOutlierResponse;
import com.example.amhs.transferjob.domain.TransferJob;
import com.example.amhs.transferjob.domain.TransferJobStatus;
import com.example.amhs.transferjob.repository.TransferJobRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Comparator;
import java.util.List;
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

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
