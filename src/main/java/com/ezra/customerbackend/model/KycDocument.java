package com.ezra.customerbackend.model;

import com.ezra.customerbackend.enums.KycDocumentType;
import com.ezra.customerbackend.enums.KycVerificationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "kyc_documents")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class KycDocument extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private KycDocumentType documentType;

    @Column(name = "file_reference", nullable = false)
    private String fileReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    private KycVerificationStatus verificationStatus;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;
}
