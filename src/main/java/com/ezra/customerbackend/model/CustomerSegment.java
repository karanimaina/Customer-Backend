package com.ezra.customerbackend.model;

import com.ezra.customerbackend.enums.SegmentOutcome;
import com.ezra.customerbackend.enums.SegmentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "customer_segments")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CustomerSegment extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "segment_type", nullable = false)
    private SegmentType segmentType;

    @Column(name = "segment_label")
    private String segmentLabel;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false)
    private SegmentOutcome outcome;

    @Column(name = "event_date")
    private Instant eventDate;
}
