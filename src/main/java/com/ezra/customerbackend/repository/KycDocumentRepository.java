package com.ezra.customerbackend.repository;

import com.ezra.customerbackend.model.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {

    List<KycDocument> findByCustomerId(Long customerId);
}
