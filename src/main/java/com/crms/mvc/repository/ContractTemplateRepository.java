package com.crms.mvc.repository;

import com.crms.mvc.entity.ContractTemplate;
import com.crms.mvc.entity.ContractTemplateStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ContractTemplateRepository extends JpaRepository<ContractTemplate, Long> {
    @EntityGraph(attributePaths = {"createdBy"})
    List<ContractTemplate> findAllByOrderByVersionNoDesc();

    Optional<ContractTemplate> findFirstByStatus(ContractTemplateStatus status);

    boolean existsByStatus(ContractTemplateStatus status);

    @Query("SELECT COALESCE(MAX(t.versionNo), 0) FROM ContractTemplate t")
    int findMaxVersionNo();
}
