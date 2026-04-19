package com.ezra.customerbackend.dto;

import jakarta.validation.constraints.NotBlank;

public record KycRejectRequest(@NotBlank String reason) {
}
