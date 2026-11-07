package com.crms.mvc.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Admin form for a DRAFT template. Rates are entered as percentages because that is how staff think about them.
 */
public class ContractTemplateFormDTO {
    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must be at most 150 characters")
    private String title;

    @NotBlank(message = "Terms content is required")
    @Size(max = 20000, message = "Terms content must be at most 20000 characters")
    private String termsContent;

    @NotNull(message = "Deposit percentage is required")
    @DecimalMin(value = "1", message = "Deposit must be at least 1%")
    @DecimalMax(value = "100", message = "Deposit cannot exceed 100%")
    private BigDecimal depositPercent;

    @NotNull(message = "Late fee percentage is required")
    @DecimalMin(value = "100", message = "Late fee must be at least 100% of the hourly rate")
    @DecimalMax(value = "999", message = "Late fee cannot exceed 999% of the hourly rate")
    private BigDecimal lateFeePercent;

    @NotNull(message = "Free cancellation window is required")
    @Min(value = 0, message = "Free cancellation window cannot be negative")
    @Max(value = 720, message = "Free cancellation window cannot exceed 720 hours")
    private Integer freeCancellationHours;

    @NotNull(message = "Free cancellation after deposit is required")
    @Min(value = 0, message = "Free cancellation after deposit cannot be negative")
    @Max(value = 24, message = "Free cancellation after deposit cannot exceed 24 hours")
    private Integer freeCancelAfterDepositHours;

    @NotNull(message = "Pickup wait is required")
    @Min(value = 1, message = "Pickup wait must be at least 1 hour")
    @Max(value = 24, message = "Pickup wait cannot exceed 24 hours")
    private Integer pickupWaitHours;

    @NotNull(message = "Compensation voucher percentage is required")
    @Min(value = 0, message = "Compensation voucher cannot be negative")
    @Max(value = 100, message = "Compensation voucher cannot exceed 100%")
    private Integer compensationVoucherPercent;

    @NotNull(message = "Compensation voucher validity is required")
    @Min(value = 1, message = "Compensation voucher must be valid at least 1 day")
    @Max(value = 365, message = "Compensation voucher cannot be valid more than 365 days")
    private Integer compensationVoucherDays;

    public static ContractTemplateFormDTO fromTemplate(ContractTemplateDTO template) {
        ContractTemplateFormDTO form = new ContractTemplateFormDTO();
        form.setTitle(template.getTitle());
        form.setTermsContent(template.getTermsContent());
        form.setDepositPercent(template.getDepositPercent());
        form.setLateFeePercent(template.getLateFeePercent());
        form.setFreeCancellationHours(template.getFreeCancellationHours());
        form.setFreeCancelAfterDepositHours(template.getFreeCancelAfterDepositHours());
        form.setPickupWaitHours(template.getPickupWaitHours());
        form.setCompensationVoucherPercent(template.getCompensationVoucherPercent());
        form.setCompensationVoucherDays(template.getCompensationVoucherDays());
        return form;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getTermsContent() { return termsContent; }
    public void setTermsContent(String termsContent) { this.termsContent = termsContent; }

    public BigDecimal getDepositPercent() { return depositPercent; }
    public void setDepositPercent(BigDecimal depositPercent) { this.depositPercent = depositPercent; }

    public BigDecimal getLateFeePercent() { return lateFeePercent; }
    public void setLateFeePercent(BigDecimal lateFeePercent) { this.lateFeePercent = lateFeePercent; }

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
}
