package com.ezra.customerbackend.service;

import com.ezra.customerbackend.dto.CreditProfile;
import com.ezra.customerbackend.dto.Customer;
import com.ezra.customerbackend.dto.KycDocument;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class CustomerMapper {
    public static Customer toCustomerResponse(com.ezra.customerbackend.model.Customer customer) {
        log.debug("Mapping customer to response: {}", customer);
        return new Customer(
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

    private static CreditProfile mapCreditProfile(com.ezra.customerbackend.model.CreditProfile profile) {
        log.debug("Mapping credit profile to response: {}", profile);
        if (profile == null) return null;
        return new CreditProfile(
                profile.getId(),
                profile.getCreditLimit(),
                profile.getOutstandingBalance(),
                profile.getCreditScore(),
                profile.getRisk(),
                profile.getTier(),
                profile.getLastEvaluatedAt()
        );
    }
    public static KycDocument toKycResponse(com.ezra.customerbackend.model.KycDocument doc) {
        log.debug("Mapping KYC document to response: {}", doc);
        return new KycDocument(
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
