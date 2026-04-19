package com.ezra.customerbackend.service;

import com.ezra.customerbackend.dto.RegisterCustomerRequest;
import com.ezra.customerbackend.enums.CustomerStatus;
import com.ezra.customerbackend.enums.KycDocumentType;
import com.ezra.customerbackend.enums.KycVerificationStatus;
import com.ezra.customerbackend.enums.PreferredChannel;
import com.ezra.customerbackend.enums.RiskLevel;
import com.ezra.customerbackend.enums.SegmentType;
import com.ezra.customerbackend.exceptions.CustomerException;
import com.ezra.customerbackend.model.CreditProfile;
import com.ezra.customerbackend.model.Customer;
import com.ezra.customerbackend.model.KycDocument;
import com.ezra.customerbackend.notification.CustomerNotificationPublisher;
import com.ezra.customerbackend.repository.CreditHistoryRepository;
import com.ezra.customerbackend.repository.CustomerRepository;
import com.ezra.customerbackend.repository.CustomerSegmentRepository;
import com.ezra.customerbackend.repository.KycDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerApplicationServiceUnitTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private KycDocumentRepository kycDocumentRepository;
    @Mock
    private CreditHistoryRepository creditHistoryRepository;
    @Mock
    private CustomerSegmentRepository customerSegmentRepository;
    @Mock
    private CustomerNotificationPublisher notificationPublisher;

    private CustomerApplicationService service;

    @BeforeEach
    void setUp() {
        service = new CustomerApplicationService(
                customerRepository,
                kycDocumentRepository,
                creditHistoryRepository,
                customerSegmentRepository,
                notificationPublisher);
        ReflectionTestUtils.setField(service, "approvedCreditLimit", 5000d);
        ReflectionTestUtils.setField(service, "approvedCreditScore", 620);
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        void persistsPendingCustomerWithZeroCreditScore() {
            when(customerRepository.existsByNationalId("NID-1")).thenReturn(false);
            when(customerRepository.existsByPhoneNumber("+100")).thenReturn(false);
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
                Customer c = inv.getArgument(0);
                c.setId(10L);
                if (c.getCreditProfile() != null) {
                    c.getCreditProfile().setId(20L);
                }
                return c;
            });

            var req = new RegisterCustomerRequest("A", "B", "NID-1", "a@b.com", "+100", PreferredChannel.EMAIL);
            var res = service.register(req);

            assertThat(res.status()).isEqualTo(CustomerStatus.PENDING);
            assertThat(res.creditProfile().creditScore()).isZero();
            assertThat(res.creditProfile().creditLimit()).isZero();

            verify(creditHistoryRepository).save(any());
            verify(notificationPublisher).publish(any());
        }

        @Test
        void duplicateNationalId_throwsBadRequest() {
            when(customerRepository.existsByNationalId("NID-1")).thenReturn(true);

            var req = new RegisterCustomerRequest("A", "B", "NID-1", "a@b.com", "+100", PreferredChannel.EMAIL);

            assertThatThrownBy(() -> service.register(req))
                    .isInstanceOf(CustomerException.class)
                    .satisfies(ex -> assertThat(((CustomerException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        }
    }

    @Nested
    @DisplayName("getCustomer")
    class GetCustomer {

        @Test
        void notFound_throws() {
            when(customerRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getCustomer(99L))
                    .isInstanceOf(CustomerException.class)
                    .satisfies(ex -> assertThat(((CustomerException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("approveKyc")
    class ApproveKyc {

        @Test
        void wrongState_throwsConflict() {
            Customer c = customer(1L, CustomerStatus.PENDING);
            when(customerRepository.findById(1L)).thenReturn(Optional.of(c));

            assertThatThrownBy(() -> service.approveKyc(1L, "rev"))
                    .isInstanceOf(CustomerException.class)
                    .satisfies(ex -> assertThat(((CustomerException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
        }

        @Test
        void underReview_updatesProfileAndPublishes() {
            Customer c = customer(1L, CustomerStatus.KYC_UNDER_REVIEW);
            KycDocument doc = new KycDocument();
            doc.setId(5L);
            doc.setDocumentType(KycDocumentType.NATIONAL_ID);
            doc.setVerificationStatus(KycVerificationStatus.UNDER_REVIEW);
            doc.setCustomer(c);
            c.getKycDocuments().add(doc);

            CreditProfile profile = c.getCreditProfile();
            profile.setId(2L);
            profile.setCreditLimit(0d);
            profile.setCreditScore(0);

            when(customerRepository.findById(1L)).thenReturn(Optional.of(c));
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

            service.approveKyc(1L, "alice");

            assertThat(c.getStatus()).isEqualTo(CustomerStatus.LOAN_READY);
            assertThat(profile.getCreditScore()).isEqualTo(620);
            assertThat(profile.getCreditLimit()).isEqualTo(5000d);

            verify(customerSegmentRepository).save(any());
            verify(creditHistoryRepository).save(any());
            verify(notificationPublisher).publish(any());
            verify(customerRepository).save(c);
        }
    }

    private static Customer customer(Long id, CustomerStatus status) {
        Customer c = Customer.builder()
                .firstName("F")
                .lastName("L")
                .nationalId("nid-" + id)
                .email("e" + id + "@t.com")
                .phoneNumber("+1" + id)
                .status(status)
                .kycStatus("K")
                .preferredChannel(PreferredChannel.EMAIL)
                .registeredAt(Instant.now())
                .build();
        c.setId(id);
        CreditProfile p = CreditProfile.builder()
                .customer(c)
                .creditLimit(0d)
                .outstandingBalance(0d)
                .creditScore(0)
                .risk(RiskLevel.STANDARD)
                .tier(SegmentType.STANDARD)
                .lastEvaluatedAt(Instant.now())
                .build();
        c.setCreditProfile(p);
        return c;
    }
}
