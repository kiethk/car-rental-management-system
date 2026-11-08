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
    /** Thời gian huỷ sau cọc: hours after paying the deposit during which cancelling refunds it in full. */
    private Integer freeCancelAfterDepositHours;
    /** Thời gian chờ nhận xe: hours after the pickup time before an unclaimed booking is cancelled. */
    private Integer pickupWaitHours;
    /** Voucher bù given when no car of the booked type can be handed over. */
    private Integer compensationVoucherPercent;
    private Integer compensationVoucherDays;
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
        dto.setFreeCancelAfterDepositHours(template.getFreeCancelAfterDepositHours());
        dto.setPickupWaitHours(template.getPickupWaitHours());
        dto.setCompensationVoucherPercent(template.getCompensationVoucherPercent());
        dto.setCompensationVoucherDays(template.getCompensationVoucherDays());
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

    /**
     * Last moment a booking under these terms can be cancelled with a full deposit refund.
     * 0 hours means the customer may cancel for free right up to pickup.
     */
    public OffsetDateTime freeCancellationDeadline(OffsetDateTime pickupTime) {
        return pickupTime.minusHours(freeCancellationHours);
    }

    /**
     * The last moment a booking under these terms can be cancelled with a full deposit refund: the later of the
     * free-cancellation deadline before pickup and the Thời gian huỷ sau cọc after the deposit was paid, the latter
     * never past the pickup time. depositPaidAt may be null while the deposit is unpaid.
     */
    public OffsetDateTime freeCancellationUntil(OffsetDateTime pickupTime, OffsetDateTime depositPaidAt) {
        OffsetDateTime until = freeCancellationDeadline(pickupTime);
        if (depositPaidAt != null && freeCancelAfterDepositHours != null && freeCancelAfterDepositHours > 0) {
            OffsetDateTime afterDeposit = depositPaidAt.plusHours(freeCancelAfterDepositHours);
            if (afterDeposit.isAfter(pickupTime)) afterDeposit = pickupTime;
            if (afterDeposit.isAfter(until)) until = afterDeposit;
        }
        return until;
    }

    /**
     * Until when a paid booking waits for its customer (Thời gian chờ nhận xe): the pickup wait after the pickup time,
     * but never past the scheduled return. After it the booking is a no-show.
     */
    public OffsetDateTime pickupDeadline(OffsetDateTime pickupTime, OffsetDateTime scheduledEndTime) {
        int wait = pickupWaitHours != null ? pickupWaitHours : 2;
        OffsetDateTime deadline = pickupTime.plusHours(wait);
        return scheduledEndTime != null && scheduledEndTime.isBefore(deadline) ? scheduledEndTime : deadline;
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

    public Integer getFreeCancelAfterDepositHours() { return freeCancelAfterDepositHours; }
    public void setFreeCancelAfterDepositHours(Integer hours) { this.freeCancelAfterDepositHours = hours; }

    public Integer getPickupWaitHours() { return pickupWaitHours; }
    public void setPickupWaitHours(Integer hours) { this.pickupWaitHours = hours; }

    public Integer getCompensationVoucherPercent() { return compensationVoucherPercent; }
    public void setCompensationVoucherPercent(Integer percent) { this.compensationVoucherPercent = percent; }

    public Integer getCompensationVoucherDays() { return compensationVoucherDays; }
    public void setCompensationVoucherDays(Integer days) { this.compensationVoucherDays = days; }

    public ContractTemplateStatus getStatus() { return status; }
    public void setStatus(ContractTemplateStatus status) { this.status = status; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getActivatedAt() { return activatedAt; }
    public void setActivatedAt(OffsetDateTime activatedAt) { this.activatedAt = activatedAt; }
}
