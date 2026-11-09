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
    @NotBlank(message = "Vui lòng nhập tiêu đề")
    @Size(max = 150, message = "Tiêu đề tối đa 150 ký tự")
    private String title;

    @NotBlank(message = "Vui lòng nhập nội dung điều khoản")
    @Size(max = 20000, message = "Nội dung điều khoản tối đa 20000 ký tự")
    private String termsContent;

    @NotNull(message = "Vui lòng nhập tỷ lệ đặt cọc")
    @DecimalMin(value = "1", message = "Tiền cọc tối thiểu 1%")
    @DecimalMax(value = "100", message = "Tiền cọc không được vượt quá 100%")
    private BigDecimal depositPercent;

    @NotNull(message = "Vui lòng nhập phí trả trễ")
    @DecimalMin(value = "100", message = "Phí trả trễ tối thiểu 100% đơn giá theo giờ")
    @DecimalMax(value = "999", message = "Phí trả trễ không được vượt quá 999% đơn giá theo giờ")
    private BigDecimal lateFeePercent;

    @NotNull(message = "Vui lòng nhập thời hạn hủy miễn phí")
    @Min(value = 0, message = "Thời hạn hủy miễn phí không được âm")
    @Max(value = 720, message = "Thời hạn hủy miễn phí không được vượt quá 720 giờ")
    private Integer freeCancellationHours;

    @NotNull(message = "Vui lòng nhập thời hạn hủy miễn phí sau khi đặt cọc")
    @Min(value = 0, message = "Thời hạn hủy miễn phí sau khi đặt cọc không được âm")
    @Max(value = 24, message = "Thời hạn hủy miễn phí sau khi đặt cọc không được vượt quá 24 giờ")
    private Integer freeCancelAfterDepositHours;

    @NotNull(message = "Vui lòng nhập thời gian chờ nhận xe")
    @Min(value = 1, message = "Thời gian chờ nhận xe tối thiểu 1 giờ")
    @Max(value = 24, message = "Thời gian chờ nhận xe không được vượt quá 24 giờ")
    private Integer pickupWaitHours;

    public static ContractTemplateFormDTO fromTemplate(ContractTemplateDTO template) {
        ContractTemplateFormDTO form = new ContractTemplateFormDTO();
        form.setTitle(template.getTitle());
        form.setTermsContent(template.getTermsContent());
        form.setDepositPercent(template.getDepositPercent());
        form.setLateFeePercent(template.getLateFeePercent());
        form.setFreeCancellationHours(template.getFreeCancellationHours());
        form.setFreeCancelAfterDepositHours(template.getFreeCancelAfterDepositHours());
        form.setPickupWaitHours(template.getPickupWaitHours());
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

}
