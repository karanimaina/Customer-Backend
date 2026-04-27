package com.ezra.customerbackend.service;

import com.ezra.customerbackend.dto.*;
import com.ezra.customerbackend.dto.CreditProfile;
import com.ezra.customerbackend.dto.Customer;
import com.ezra.customerbackend.dto.KycDocument;
import com.ezra.customerbackend.enums.*;
import com.ezra.customerbackend.exceptions.CustomerException;
import com.ezra.customerbackend.model.*;
import com.ezra.customerbackend.notification.CustomerNotificationEvent;
import com.ezra.customerbackend.notification.CustomerNotificationPublisher;
import com.ezra.customerbackend.repository.CreditHistoryRepository;
import com.ezra.customerbackend.repository.CustomerRepository;
import com.ezra.customerbackend.repository.CustomerSegmentRepository;
import com.ezra.customerbackend.repository.KycDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerApplicationService {

    private final CustomerRepository customerRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final CreditHistoryRepository creditHistoryRepository;
    private final CustomerSegmentRepository customerSegmentRepository;
    private final CustomerNotificationPublisher notificationPublisher;

    @Value("${customer.kyc.approved-credit-limit:5000}")
    private double approvedCreditLimit;

    @Value("${customer.kyc.approved-credit-score:620}")
    private int approvedCreditScore;

    @Transactional(readOnly = true)
    public Customer getCustomer(Long id) {
        return CustomerMapper.toCustomerResponse(loadCustomer(id));
    }

    /**
     * Self-registration: validate identifiers, create PENDING account and credit profile with score 0 (flowchart).
     */
    @Transactional

    public Customer register(RegisterCustomerRequest request) {
        log.info("Registering customer: {}", request);
        validateUniqueIdentifiers(request.nationalId(), request.email(), request.phoneNumber());
        com.ezra.customerbackend.model.Customer customer = com.ezra.customerbackend.model.Customer.builder()
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .nationalId(request.nationalId().trim())
                .email(normalizeToNull(request.email()))
                .phoneNumber(request.phoneNumber().trim())
                .status(CustomerStatus.PENDING)
                .kycStatus("AWAITING_NATIONAL_ID")
                .preferredChannel(request.preferredChannel())
                .registeredAt(Instant.now())
                .build();

        Double DOUBLE_VALUE = 0.00;
        com.ezra.customerbackend.model.CreditProfile profile = com.ezra.customerbackend.model.CreditProfile.builder()
                .customer(customer)
                .creditLimit(DOUBLE_VALUE)
                .outstandingBalance(DOUBLE_VALUE)
                .creditScore(0)
                .risk(RiskLevel.STANDARD)
                .tier(SegmentType.STANDARD)
                .lastEvaluatedAt(Instant.now())
                .build();
        log.info("Created credit profile: {}", profile);
        customer.setCreditProfile(profile);
        com.ezra.customerbackend.model.Customer saved = customerRepository.save(customer);
        log.info("Saved customer: {}", saved);
        recordHistory(saved, CreditHistoryEventType.ACCOUNT_REGISTERED, HistoryOutcome.SUCCESS, null);
        publish(saved.getId(), "CUSTOMER_REGISTERED", "Account created; status PENDING; credit score 0.");
        return CustomerMapper.toCustomerResponse(saved);
    }

    @Transactional
    public KycDocument uploadKycDocument(Long customerId,
                                         KycDocumentType documentType,
                                         FilePart file) {
        log.info("Uploading KYC document for customer {}: {}", customerId, documentType);
        com.ezra.customerbackend.model.Customer customer = loadCustomer(customerId);
        validateKycState(customer);
        String filePath = storeFile(customerId, file);
        log.info("Stored file at: {}", filePath);
        com.ezra.customerbackend.model.KycDocument doc = new com.ezra.customerbackend.model.KycDocument();
        doc.setCustomer(customer);
        doc.setDocumentType(documentType);
        doc.setFileReference(filePath); // ✅ store path
        doc.setVerificationStatus(KycVerificationStatus.PENDING);
        doc.setSubmittedAt(Instant.now());
        log.info("Created KYC document: {}", doc);
        customer.getKycDocuments().add(doc);
        log.info("Added to customer's KYC documents: {}", customer.getKycDocuments());
        if (documentType == KycDocumentType.NATIONAL_ID) {
            customer.setKycStatus("NATIONAL_ID_RECEIVED");
        }
        log.info("Updated customer's KYC status: {}", customer.getKycStatus());
        com.ezra.customerbackend.model.KycDocument saved = kycDocumentRepository.save(doc);
        log.info("Saved KYC document: {}", saved);
        publish(customerId, "KYC_DOCUMENT_UPLOADED",
                "Document uploaded: " + documentType);
        log.info("Published notification");
        return CustomerMapper.toKycResponse(saved);
    }

    public void validateKycState(com.ezra.customerbackend.model.Customer customer) {
        log.info("Validating KYC state for customer: {}", customer);
        if (customer.getStatus() == CustomerStatus.LOAN_READY) {
            log.info("Customer is already LOAN_READY; cannot upload more documents.");
            throw new CustomerException(HttpStatus.CONFLICT, "KYC already approved for this customer.");
        }
        if (customer.getStatus() == CustomerStatus.KYC_UNDER_REVIEW) {
            log.info("KYC is under review; uploads are locked.");
            throw new CustomerException(HttpStatus.CONFLICT, "KYC is under review; uploads are locked.");
        }

        if (customer.getStatus() == CustomerStatus.KYC_REJECTED) {
            customer.setStatus(CustomerStatus.PENDING);
            log.info("KYC is rejected; status reset to PENDING.");
            customer.setKycStatus("AWAITING_NATIONAL_ID");
        }

    }

    private String storeFile(Long customerId, FilePart file) {
        try {
            log.info("Storing file for customer {}: {}", customerId, file);
            String uploadDir = "uploads/" + customerId;
            Files.createDirectories(Paths.get(uploadDir));
            log.info("Created directory: {}", uploadDir);
            String original = file.filename() != null ? file.filename() : "upload";
            String fileName = UUID.randomUUID() + "_" + original;
            Path filePath = Paths.get(uploadDir, fileName).toAbsolutePath().normalize();
            log.info("Resolved file path: {}", filePath);
            file.transferTo(filePath).block(Duration.ofMinutes(2));
            log.info("File stored successfully");
            return filePath.toString();
        } catch (Exception e) {
            throw new CustomerException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file");
        }
    }

    @Transactional
    public Customer submitKycForReview(Long customerId) {
        log.info("Submitting KYC for review for customer: {}", customerId);
        com.ezra.customerbackend.model.Customer customer = loadCustomer(customerId);
        if (customer.getStatus() != CustomerStatus.PENDING) {
            log.info("Customer is not in PENDING state; cannot submit KYC.");
            throw new CustomerException(HttpStatus.CONFLICT, "Customer must be in PENDING state to submit KYC.");
        }
        boolean hasNationalId = customer.getKycDocuments().stream()
                .anyMatch(d -> d.getDocumentType() == KycDocumentType.NATIONAL_ID
                        && d.getVerificationStatus() == KycVerificationStatus.PENDING);
        log.info("Customer has national ID: {}", hasNationalId);
        if (!hasNationalId) {
            log.info("Customer does not have a national ID; cannot submit KYC.");
            throw new CustomerException(HttpStatus.BAD_REQUEST, "National ID document is required before review.");
        }

        customer.getKycDocuments().stream()
                .filter(d -> d.getVerificationStatus() == KycVerificationStatus.PENDING)
                .forEach(d -> d.setVerificationStatus(KycVerificationStatus.UNDER_REVIEW));
        log.info("KYC documents marked as under review: {}", customer.getKycDocuments());
        customer.setStatus(CustomerStatus.KYC_UNDER_REVIEW);
        customer.setKycStatus("KYC_UNDER_REVIEW");
        log.info("Customer status updated to KYC_UNDER_REVIEW");
        recordHistory(customer, CreditHistoryEventType.KYC_SUBMITTED, HistoryOutcome.PENDING, null);
        publish(customerId, "KYC_UNDER_REVIEW", "KYC submitted for review.");
        log.info("Published notification");
        return CustomerMapper.toCustomerResponse(customerRepository.save(customer));
    }

    @Transactional
    public Customer approveKyc(Long customerId, String reviewer) {
        com.ezra.customerbackend.model.Customer customer = loadCustomer(customerId);
        log.info("Approving KYC for customer: {}", customerId);
        if (customer.getStatus() != CustomerStatus.KYC_UNDER_REVIEW) {
            log.info("Customer is not in KYC_UNDER_REVIEW state; cannot approve KYC.");
            throw new CustomerException(HttpStatus.CONFLICT, "KYC is not under review.");
        }
        Instant now = Instant.now();
        customer.getKycDocuments().forEach(d -> {
            if (d.getVerificationStatus() == KycVerificationStatus.UNDER_REVIEW) {
                d.setVerificationStatus(KycVerificationStatus.VERIFIED);
                d.setVerifiedAt(now);
            }
        });
      log.info("KYC documents marked as verified: {}", customer.getKycDocuments());
        com.ezra.customerbackend.model.CreditProfile profile = Objects.requireNonNull(customer.getCreditProfile(), "Credit profile missing");
        double previousLimit = profile.getCreditLimit() == null ? 0d : profile.getCreditLimit();
        profile.setCreditScore(approvedCreditScore);
        profile.setCreditLimit(approvedCreditLimit);
        profile.setRisk(RiskLevel.STANDARD);
        profile.setTier(SegmentType.STANDARD);
        profile.setLastEvaluatedAt(now);
        profile.setRejectionReason(null);
        log.info("Credit profile updated: {}", profile);
        LoanLimitAdjustment adjustment = new LoanLimitAdjustment();
        adjustment.setCreditProfile(profile);
        adjustment.setPreviousLimit(previousLimit);
        adjustment.setNewLimit(approvedCreditLimit);
        adjustment.setReason("KYC approved — initial limit assigned.");
        adjustment.setAdjustedBy(reviewer);
        adjustment.setAdjustedAt(now);
        profile.getLimitAdjustments().add(adjustment);
        log.info("Loan limit adjustment added: {}", adjustment);
        CustomerSegment segment = new CustomerSegment();
        segment.setCustomer(customer);
        segment.setSegmentType(SegmentType.STANDARD);
        segment.setSegmentLabel("STANDARD_RETAIL");
        segment.setAssignedAt(now);
        segment.setOutcome(SegmentOutcome.ACTIVE);
        segment.setEventDate(now);
        customerSegmentRepository.save(segment);
        log.info("Customer segment added: {}", segment);
        customer.setStatus(CustomerStatus.LOAN_READY);
        customer.setKycStatus("VERIFIED");
        log.info("Customer status updated to LOAN_READY");
        recordHistory(customer, CreditHistoryEventType.KYC_APPROVED, HistoryOutcome.SUCCESS, approvedCreditLimit);
        publish(customerId, "KYC_APPROVED", "KYC verified; credit profile initialised; eligible for loan disbursement.");
        log.info("Published notification");
        return CustomerMapper.toCustomerResponse(customerRepository.save(customer));
    }

    @Transactional
    public Customer rejectKyc(Long customerId, KycRejectRequest request) {
        log.info("Rejecting KYC for customer: {}", customerId);
        com.ezra.customerbackend.model.Customer customer = loadCustomer(customerId);
        if (customer.getStatus() != CustomerStatus.KYC_UNDER_REVIEW) {
            log.info("Customer is not in KYC_UNDER_REVIEW state; cannot reject KYC.");
            throw new CustomerException(HttpStatus.CONFLICT, "KYC is not under review.");
        }
        String reason = request.reason().trim();
        log.info("Rejection reason: {}", reason);
        customer.getKycDocuments().stream()
                .filter(d -> d.getVerificationStatus() == KycVerificationStatus.UNDER_REVIEW)
                .forEach(d -> {
                    d.setVerificationStatus(KycVerificationStatus.REJECTED);
                    d.setRejectionReason(reason);
                });
        log.info("KYC documents marked as rejected: {}", customer.getKycDocuments());
        com.ezra.customerbackend.model.CreditProfile profile = customer.getCreditProfile();
        if (profile != null) {
            profile.setRejectionReason(reason);
        }
        log.info("Credit profile updated: {}", profile);
        customer.setStatus(CustomerStatus.KYC_REJECTED);
        customer.setKycStatus("REJECTED");
        log.info("Customer status updated to KYC_REJECTED");
        recordHistory(customer, CreditHistoryEventType.KYC_REJECTED, HistoryOutcome.FAILURE, null);
        publish(customerId, "KYC_REJECTED", "KYC rejected: " + reason);
        log.info("Published notification");
        return CustomerMapper.toCustomerResponse(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public List<KycDocument> listKycDocuments(Long customerId) {
        loadCustomer(customerId);
        log.info("Listing KYC documents for customer: {}", customerId);
        return kycDocumentRepository.findByCustomerId(customerId).stream()
                .map(CustomerMapper::toKycResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CreditProfile getCreditProfile(Long customerId) {
        log.info("Retrieving credit profile for customer: {}", customerId);
        com.ezra.customerbackend.model.Customer customer = loadCustomer(customerId);
        log.info("Loaded customer: {}", customer);
        com.ezra.customerbackend.model.CreditProfile profile = Objects.requireNonNull(customer.getCreditProfile(), "Credit profile missing");
        log.info("Loaded credit profile: {}", profile);
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

    private com.ezra.customerbackend.model.Customer loadCustomer(Long id) {
        log.info("Loading customer: {}", id);
        return customerRepository.findById(id)
                .orElseThrow(() -> new CustomerException(HttpStatus.NOT_FOUND, "Customer not found"));
    }

    private void validateUniqueIdentifiers(String nationalId, String email, String phone) {
        log.info("Validating unique identifiers: nationalId={}, email={}, phone={}", nationalId, email, phone);
        if (customerRepository.existsByNationalId(nationalId.trim())) {
            log.info("National ID already registered");
            throw new CustomerException(HttpStatus.BAD_REQUEST, "National ID already registered.");
        }
        String e = normalizeToNull(email);
        log.info("Normalized email: {}", e);
        if (e != null && customerRepository.existsByEmail(e)) {
            log.info("Email already registered");
            throw new CustomerException(HttpStatus.BAD_REQUEST, "Email already registered.");
        }
        if (customerRepository.existsByPhoneNumber(phone.trim())) {
            log.info("Phone number already registered");
            throw new CustomerException(HttpStatus.BAD_REQUEST, "Phone number already registered.");
        }
    }

    private static String normalizeToNull(String value) {
        log.info("Normalizing value: {}", value);
        if (value == null) {
            log.info("Value is null; returning null");
            return null;
        }
        String t = value.trim();
        log.info("Normalized value: {}", t);
        return t.isEmpty() ? null : t;
    }

    private void recordHistory(com.ezra.customerbackend.model.Customer customer, CreditHistoryEventType type, HistoryOutcome outcome, Double amount) {
        log.info("Recording history for customer {}: type={}, outcome={}, amount={}", customer.getId(), type, outcome, amount);
        CreditHistory history = new CreditHistory();
        history.setCustomer(customer);
        history.setEventType(type);
        history.setLoanId(null);
        history.setOutcome(outcome);
        history.setAmount(amount);
        history.setEventDate(Instant.now());
        creditHistoryRepository.save(history);
        log.info("History recorded: {}", history);
    }

    private void publish(Long customerId, String type, String message) {
        log.info("Publishing notification for customer {}: type={}, message={}", customerId, type, message);
        notificationPublisher.publish(new CustomerNotificationEvent(type, customerId, message, Instant.now()));
    }
}
