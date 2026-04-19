package com.ezra.customerbackend.service;

import com.ezra.customerbackend.model.CreditProfile;
import com.ezra.customerbackend.model.Customer;
import com.ezra.customerbackend.model.KycDocument;
import com.ezra.customerbackend.dto.CreditProfileResponse;
import com.ezra.customerbackend.dto.CustomerResponse;
import com.ezra.customerbackend.dto.KycDocumentResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class CustomerMapper {

    private CustomerMapper() {
    }

    public static CustomerResponse toCustomerResponse(Customer customer) {
        log.debug("Mapping customer to response: {}", customer);
        return new CustomerResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getNationalId(),
                customer.getEmail(),
                customer.getPhoneNumber(),
                customer.getKycStatus(),
                customer.getStatus(),
                customer.getPreferredChannel(),
                customer.getRegisteredAt(),
                mapCreditProfile(customer.getCreditProfile())
        );
    }

    private static CreditProfileResponse mapCreditProfile(CreditProfile profile) {
        log.debug("Mapping credit profile to response: {}", profile);
        if (profile == null) return null;
        return new CreditProfileResponse(
                profile.getId(),
                profile.getCreditLimit(),
                profile.getOutstandingBalance(),
                profile.getCreditScore(),
                profile.getRisk(),
                profile.getTier(),
                profile.getLastEvaluatedAt()
        );
    }
    public static KycDocumentResponse toKycResponse(KycDocument doc) {
        log.debug("Mapping KYC document to response: {}", doc);
        return new KycDocumentResponse(
                doc.getId(),
                doc.getDocumentType(),
                doc.getFileReference(),
                doc.getVerificationStatus(),
                doc.getRejectionReason(),
                doc.getSubmittedAt(),
                doc.getVerifiedAt()
        );
    }
}
