package com.ezra.customerbackend.dto;

import com.ezra.customerbackend.enums.KycDocumentType;
import com.ezra.customerbackend.enums.KycVerificationStatus;

import java.time.Instant;

public record KycDocumentResponse(
        Long id,
        KycDocumentType documentType,
        String fileReference,
        KycVerificationStatus verificationStatus,
        String rejectionReason,
        Instant submittedAt,
        Instant verifiedAt
) {
}
