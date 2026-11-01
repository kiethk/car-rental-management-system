package com.crms.mvc.repository;

import com.crms.mvc.entity.LicenseDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LicenseDocumentRepository extends JpaRepository<LicenseDocument, Long> {
    Optional<LicenseDocument> findFirstByCustomer_IdOrderBySubmittedAtDesc(Long customerId);
}
