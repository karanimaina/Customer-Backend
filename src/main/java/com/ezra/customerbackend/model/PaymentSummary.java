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
@Table(name = "payment_summaries")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentSummary extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "loan_id")
    private Long loanId;

    @Column(name = "total_loans", nullable = false)
    private Integer totalLoans;

    @Column(name = "on_time_count", nullable = false)
    private Integer onTimeCount;

    @Column(name = "late_count", nullable = false)
    private Integer lateCount;

    @Column(name = "defaulted_count", nullable = false)
    private Integer defaultedCount;

    @Column(name = "total_repaid", nullable = false)
    private Integer totalRepaid;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;
}
