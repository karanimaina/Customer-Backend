package com.ezra.customerbackend.dto;

import com.ezra.customerbackend.enums.PreferredChannel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterCustomerRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String nationalId,
        @Email String email,
        @NotBlank String phoneNumber,
        @NotNull PreferredChannel preferredChannel
) {
}
