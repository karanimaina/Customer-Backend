package com.ezra.customerbackend.repository;

import com.ezra.customerbackend.model.CreditHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditHistoryRepository extends JpaRepository<CreditHistory, Long> {
}
