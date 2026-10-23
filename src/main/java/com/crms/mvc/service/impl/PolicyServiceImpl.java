package com.crms.mvc.service.impl;

import com.crms.mvc.entity.Policy;
import com.crms.mvc.repository.PolicyRepository;
import com.crms.mvc.service.PolicyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class PolicyServiceImpl implements PolicyService {
    private final PolicyRepository policyRepository;

    public PolicyServiceImpl(PolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Policy> getAllPolicies() {
        return policyRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Policy getPolicyByCode(String policyCode) {
        return policyRepository.findByPolicyCode(policyCode)
                .orElseThrow(() -> new NoSuchElementException("Policy not found with code: " + policyCode));
    }

    @Override
    @Transactional(readOnly = true)
    public String buildTermsSnapshot() {
        List<Policy> policies = policyRepository.findAll();
        if (policies.isEmpty()) {
            return "DRIVENOW CAR RENTAL AGREEMENT TERMS: Commitment to standard service regulations.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("DRIVENOW SELF-DRIVE CAR RENTAL SERVICE AGREEMENT\n");
        sb.append("Governed by active system policies at confirmation time:\n\n");
        int index = 1;
        for (Policy p : policies) {
            sb.append(index++).append(". ").append(p.getTitle()).append(":\n")
              .append("   ").append(p.getContent()).append("\n\n");
        }
        return sb.toString().trim();
    }

    @Override
    public Policy updatePolicy(String policyCode, String title, String content) {
        Policy policy = policyRepository.findByPolicyCode(policyCode)
                .orElseThrow(() -> new NoSuchElementException("Policy not found with code: " + policyCode));
        if (title != null && !title.trim().isEmpty()) {
            policy.setTitle(title.trim());
        }
        if (content != null && !content.trim().isEmpty()) {
            policy.setContent(content.trim());
        }
        policy.setUpdatedAt(OffsetDateTime.now());
        return policyRepository.save(policy);
    }
}
