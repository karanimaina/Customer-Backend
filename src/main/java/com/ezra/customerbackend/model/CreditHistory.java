package com.ezra.customerbackend.model;

import com.ezra.customerbackend.enums.CreditHistoryEventType;
import com.ezra.customerbackend.enums.HistoryOutcome;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "credit_history")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreditHistory extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "loan_id")
    private Long loanId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private CreditHistoryEventType eventType;

    @Column(name = "amount")
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false)
    private HistoryOutcome outcome;

    @Column(name = "event_date", nullable = false)
    private Instant eventDate;
}
