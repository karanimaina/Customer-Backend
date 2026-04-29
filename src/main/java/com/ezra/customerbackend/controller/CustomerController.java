package com.ezra.customerbackend.controller;

import com.ezra.customerbackend.dto.*;
import com.ezra.customerbackend.enums.KycDocumentType;
import com.ezra.customerbackend.service.CustomerApplicationService;
import com.ezra.customerbackend.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")

@RequiredArgsConstructor
public class CustomerController {

    private final CustomerApplicationService customerApplicationService;

    @PostMapping("/register")
    public Mono<ResponseEntity<ApiResponse<Customer>>> register(@Valid @RequestBody RegisterCustomerRequest request) {
        return Mono.fromCallable(() -> customerApplicationService.register(request))
                .subscribeOn(Schedulers.boundedElastic())
                .map(data -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.created("Customer registered successfully", data)));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<Customer>>> get(@PathVariable Long id) {
        return Mono.fromCallable(() -> customerApplicationService.getCustomer(id))
                .subscribeOn(Schedulers.boundedElastic())
                .map(data -> ResponseEntity.ok(ApiResponse.ok("Customer retrieved successfully", data)));
    }

    @GetMapping("/{id}/credit-profile")
    public Mono<ResponseEntity<ApiResponse<CreditProfile>>> creditProfile(@PathVariable Long id) {
        return Mono.fromCallable(() -> customerApplicationService.getCreditProfile(id))
                .subscribeOn(Schedulers.boundedElastic())
                .map(data -> ResponseEntity.ok(ApiResponse.ok("Credit profile retrieved successfully", data)));
    }

    @PostMapping(value = "/{id}/kyc/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<ApiResponse<KycDocument>>> uploadKyc(
            @PathVariable Long id,
            @RequestPart("documentType") String documentType,
            @RequestPart("file") FilePart file) {

        return Mono.fromCallable(() -> {
                    String raw = documentType == null ? "" : documentType.trim();
                    if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() >= 2) {
                        raw = raw.substring(1, raw.length() - 1).trim();
                    }
                    KycDocumentType kycDocumentType = KycDocumentType.valueOf(raw);
                    return customerApplicationService.uploadKycDocument(id, kycDocumentType, file);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .map(data -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.created("KYC document uploaded successfully", data)));
    }

    @GetMapping("/{custmerid}/list/kyc/documents")
    public Mono<ResponseEntity<ApiResponse<List<KycDocument>>>> listKyc(@PathVariable Long custmerid) {
        return Mono.fromCallable(() -> customerApplicationService.listKycDocuments(custmerid))
                .subscribeOn(Schedulers.boundedElastic())
                .map(data -> ResponseEntity.ok(ApiResponse.ok("KYC documents retrieved successfully", data)));
    }

    @PostMapping("/{customerid}/kyc/submit")
    public Mono<ResponseEntity<ApiResponse<Customer>>> submitKyc(@PathVariable Long customerid) {
        return Mono.fromCallable(() -> customerApplicationService.submitKycForReview(customerid))
                .subscribeOn(Schedulers.boundedElastic())
                .map(data -> ResponseEntity.ok(ApiResponse.ok("KYC submitted for review successfully", data)));
    }


    @PostMapping("/{customerid}/kyc/approve")
    public Mono<ResponseEntity<ApiResponse<Customer>>> approveKyc(
            @PathVariable Long customerid,
            @RequestHeader(value = "X-Reviewer") String reviewer) {
        return Mono.fromCallable(() -> customerApplicationService.approveKyc(customerid, reviewer))
                .subscribeOn(Schedulers.boundedElastic())
                .map(data -> ResponseEntity.ok(ApiResponse.ok("KYC approved successfully", data)));
    }

    @PostMapping("/{id}/kyc/reject")
    public Mono<ResponseEntity<ApiResponse<Customer>>> rejectKyc(
            @PathVariable Long id,
            @Valid @RequestBody KycRejectRequest request) {
        return Mono.fromCallable(() -> customerApplicationService.rejectKyc(id, request))
                .subscribeOn(Schedulers.boundedElastic())
                .map(data -> ResponseEntity.ok(ApiResponse.ok("KYC rejected successfully", data)));
    }
}
