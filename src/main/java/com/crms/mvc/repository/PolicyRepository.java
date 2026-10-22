package com.crms.mvc.repository;

import com.crms.mvc.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PolicyRepository extends JpaRepository<Policy, Long> {
    Optional<Policy> findByPolicyCode(String policyCode);
}
