package com.ezra.customerbackend.model;

import com.ezra.customerbackend.enums.RiskLevel;
import com.ezra.customerbackend.enums.SegmentType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "credit_profiles")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreditProfile extends BaseEntity {

    @OneToOne(optional = false)
    @JoinColumn(name = "customer_id", nullable = false, unique = true)
    private Customer customer;

    @Column(name = "credit_limit")
    private Double creditLimit;

    @Column(name = "outstanding_balance")
    private Double outstandingBalance;

    @Column(name = "credit_score", nullable = false)
    private Integer creditScore;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk", nullable = false)
    private RiskLevel risk;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false)
    private SegmentType tier;

    @Column(name = "last_evaluated_at")
    private Instant lastEvaluatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "creditProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LoanLimitAdjustment> limitAdjustments = new ArrayList<>();
}
