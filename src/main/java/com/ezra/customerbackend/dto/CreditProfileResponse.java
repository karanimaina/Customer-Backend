package com.ezra.customerbackend.dto;

import com.ezra.customerbackend.enums.RiskLevel;
import com.ezra.customerbackend.enums.SegmentType;

import java.time.Instant;

public record CreditProfileResponse(
        Long id,
        Double creditLimit,
        Double outstandingBalance,
        Integer creditScore,
        RiskLevel risk,
        SegmentType tier,
        Instant lastEvaluatedAt
) {
}
