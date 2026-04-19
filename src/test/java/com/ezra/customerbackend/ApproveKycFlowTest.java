package com.ezra.customerbackend;

import com.ezra.customerbackend.model.Customer;
import com.ezra.customerbackend.model.KycDocument;
import com.ezra.customerbackend.enums.KycDocumentType;
import com.ezra.customerbackend.enums.KycVerificationStatus;
import com.ezra.customerbackend.enums.PreferredChannel;
import com.ezra.customerbackend.repository.CustomerRepository;
import com.ezra.customerbackend.repository.KycDocumentRepository;
import com.ezra.customerbackend.service.CustomerApplicationService;
import com.ezra.customerbackend.dto.RegisterCustomerRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@Transactional
@DisplayName("KYC approve flow (service + JPA + H2)")
class ApproveKycFlowTest {

    @Autowired
    CustomerApplicationService customerApplicationService;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    KycDocumentRepository kycDocumentRepository;

    @Test
    @DisplayName("register → national ID doc → submit → approve completes without error")
    void approveKyc_fullFlow() {
        var reg = new RegisterCustomerRequest(
                "A", "B", "nid-flow-1", "flow@b.com", "+19990002", PreferredChannel.EMAIL);
        var created = customerApplicationService.register(reg);
        Long id = created.id();

        Customer managed = customerRepository.findById(id).orElseThrow();
        KycDocument doc = new KycDocument();
        doc.setCustomer(managed);
        doc.setDocumentType(KycDocumentType.NATIONAL_ID);
        doc.setFileReference("ref");
        doc.setVerificationStatus(KycVerificationStatus.PENDING);
        doc.setSubmittedAt(Instant.now());
        managed.getKycDocuments().add(doc);
        kycDocumentRepository.save(doc);

        customerApplicationService.submitKycForReview(id);

        assertThatCode(() -> customerApplicationService.approveKyc(id, "reviewer"))
                .doesNotThrowAnyException();
    }
}
