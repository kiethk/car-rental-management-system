package com.crms.mvc.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "contract_templates")
public class ContractTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contract_template_id")
    private Long id;

    @Column(name = "version_no", nullable = false, unique = true)
    private Integer versionNo;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "terms_content", nullable = false, columnDefinition = "TEXT")
    private String termsContent;

    @Column(name = "deposit_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal depositRate;

    @Column(name = "late_fee_multiplier", nullable = false, precision = 5, scale = 2)
    private BigDecimal lateFeeMultiplier;

    @Column(name = "free_cancellation_hours", nullable = false)
    private Integer freeCancellationHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ContractTemplateStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "created_by", nullable = true)
    private Employee createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "activated_at")
    private OffsetDateTime activatedAt;

    public boolean isEditable() {
        return status == ContractTemplateStatus.DRAFT;
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

    public Employee getCreatedBy() { return createdBy; }
    public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getActivatedAt() { return activatedAt; }
    public void setActivatedAt(OffsetDateTime activatedAt) { this.activatedAt = activatedAt; }
}
