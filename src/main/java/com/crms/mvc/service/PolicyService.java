package com.crms.mvc.service;

import com.crms.mvc.entity.Policy;
import java.util.List;

public interface PolicyService {
    List<Policy> getAllPolicies();
    Policy getPolicyByCode(String policyCode);
    String buildTermsSnapshot();
    Policy updatePolicy(String policyCode, String title, String content);
}
