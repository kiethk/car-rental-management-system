package com.crms.mvc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class LicenseApprovalDTO {
    @NotBlank(message = "Decision is required")
    @Pattern(regexp = "approved|rejected", message = "Decision is invalid")
    private String decision;

    @Size(max = 500, message = "Rejection reason must be at most 500 characters")
    private String rejectionReason;

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
