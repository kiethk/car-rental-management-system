package com.crms.mvc.repository;

import com.crms.mvc.entity.IssueEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IssueEvidenceRepository extends JpaRepository<IssueEvidence, Long> {
    long countByIssueId(Long issueId);

    Optional<IssueEvidence> findByIdAndIssueId(Long id, Long issueId);
}
