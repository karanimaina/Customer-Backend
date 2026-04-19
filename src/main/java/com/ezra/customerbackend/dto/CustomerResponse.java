package com.ezra.customerbackend.dto;

import com.ezra.customerbackend.enums.CustomerStatus;
import com.ezra.customerbackend.enums.PreferredChannel;

import java.time.Instant;

public record CustomerResponse(
        Long id,
        String firstName,
        String lastName,
        String nationalId,
        String email,
        String phoneNumber,
        String kycStatus,
        CustomerStatus status,
        PreferredChannel preferredChannel,
        Instant registeredAt,
        CreditProfileResponse creditProfile
) {
}
