package com.crms.mvc.dto;

import com.crms.mvc.entity.ContractTemplate;
import com.crms.mvc.entity.ContractTemplateStatus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class ContractTemplateDTO implements Serializable {
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private Long id;
    private Integer versionNo;
    private String title;
    private String termsContent;
    private BigDecimal depositRate;
    private BigDecimal lateFeeMultiplier;
    private Integer freeCancellationHours;
    private ContractTemplateStatus status;
    private String createdByName;
    private OffsetDateTime createdAt;
    private OffsetDateTime activatedAt;

    public static ContractTemplateDTO fromEntity(ContractTemplate template) {
        ContractTemplateDTO dto = new ContractTemplateDTO();
        dto.setId(template.getId());
        dto.setVersionNo(template.getVersionNo());
        dto.setTitle(template.getTitle());
        dto.setTermsContent(template.getTermsContent());
        dto.setDepositRate(template.getDepositRate());
        dto.setLateFeeMultiplier(template.getLateFeeMultiplier());
        dto.setFreeCancellationHours(template.getFreeCancellationHours());
        dto.setStatus(template.getStatus());
        dto.setCreatedByName(template.getCreatedBy() != null ? template.getCreatedBy().getName() : null);
        dto.setCreatedAt(template.getCreatedAt());
        dto.setActivatedAt(template.getActivatedAt());
        return dto;
    }

    /** Deposit as a percentage for display, e.g. 0.5000 -> 50. */
    public BigDecimal getDepositPercent() {
        return toPercent(depositRate);
    }

    /** Late fee per hour as a percentage of the hourly rate, e.g. 2.00 -> 200. */
    public BigDecimal getLateFeePercent() {
        return toPercent(lateFeeMultiplier);
    }

    public boolean isEditable() {
        return status == ContractTemplateStatus.DRAFT;
    }

    private static BigDecimal toPercent(BigDecimal rate) {
        BigDecimal percent = rate.multiply(ONE_HUNDRED).stripTrailingZeros();
        // stripTrailingZeros turns 50.0000 into 5E+1; force a plain scale so pages print "50".
        return percent.scale() < 0 ? percent.setScale(0) : percent;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getTermsContent() { return termsContent; }
    public void setTermsContent(String termsContent) { this.termsContent = termsContent; }

    public BigDecimal getDepositRate() { return depositRate; }
    public void setDepositRate(BigDecimal depositRate) { this.depositRate = depositRate; }

    public BigDecimal getLateFeeMultiplier() { return lateFeeMultiplier; }
    public void setLateFeeMultiplier(BigDecimal lateFeeMultiplier) { this.lateFeeMultiplier = lateFeeMultiplier; }

    public Integer getFreeCancellationHours() { return freeCancellationHours; }
    public void setFreeCancellationHours(Integer freeCancellationHours) { this.freeCancellationHours = freeCancellationHours; }

    public ContractTemplateStatus getStatus() { return status; }
    public void setStatus(ContractTemplateStatus status) { this.status = status; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getActivatedAt() { return activatedAt; }
    public void setActivatedAt(OffsetDateTime activatedAt) { this.activatedAt = activatedAt; }
}
