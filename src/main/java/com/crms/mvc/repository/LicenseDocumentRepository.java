package com.crms.mvc.repository;

import com.crms.mvc.entity.LicenseDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LicenseDocumentRepository extends JpaRepository<LicenseDocument, Long> {
    List<LicenseDocument> findByCustomer_IdOrderBySubmittedAtDesc(Long customerId);

    boolean existsByCustomer_IdAndStatus(Long customerId, String status);

    List<LicenseDocument> findByStatusOrderBySubmittedAtAsc(String status);
}
