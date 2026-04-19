package com.ezra.customerbackend.dto;

import com.ezra.customerbackend.enums.KycDocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record KycDocumentUploadRequest(
        @NotNull KycDocumentType documentType,
        @NotBlank MultipartFile fileReference
) {
}
