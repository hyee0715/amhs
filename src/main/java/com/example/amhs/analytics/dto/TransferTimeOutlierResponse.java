package com.example.amhs.analytics.dto;

import java.util.List;

public record TransferTimeOutlierResponse(
        Long jobCount,
        Double q1,
        Double q3,
        Double iqr,
        Double outlierThreshold,
        Long outlierCount,
        List<TransferTimeOutlierJobResponse> outlierJobs
) {
    public static TransferTimeOutlierResponse insufficientData(long jobCount) {
        return new TransferTimeOutlierResponse(
                jobCount,
                null,
                null,
                null,
                null,
                0L,
                List.of()
        );
    }
}
