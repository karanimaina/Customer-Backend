package com.ezra.customerbackend.repository;

import com.ezra.customerbackend.model.CustomerSegment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerSegmentRepository extends JpaRepository<CustomerSegment, Long> {
}
