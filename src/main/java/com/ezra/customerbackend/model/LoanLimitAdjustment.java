package com.ezra.customerbackend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "loan_limit_adjustments")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LoanLimitAdjustment extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "credit_profile_id", nullable = false)
    private CreditProfile creditProfile;

    @Column(name = "previous_limit", nullable = false)
    private Double previousLimit;

    @Column(name = "new_limit", nullable = false)
    private Double newLimit;

    @Column(name = "reason")
    private String reason;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "adjusted_by")
    private String adjustedBy;

    @Column(name = "adjusted_at", nullable = false)
    private Instant adjustedAt;
}
