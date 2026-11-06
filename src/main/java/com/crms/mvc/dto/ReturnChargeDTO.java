package com.crms.mvc.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** One Return charge row (cleaning, fuel shortfall, toll, other) entered when the vehicle comes back. */
public class ReturnChargeDTO {
    private String feeType;

    @DecimalMin(value = "0", message = "Phí trả xe không được âm")
    @DecimalMax(value = "999999999999", message = "Phí trả xe quá lớn")
    private BigDecimal amount;

    @Size(max = 200, message = "Mô tả phí trả xe tối đa 200 ký tự")
    private String description;

    public String getFeeType() { return feeType; }
    public void setFeeType(String feeType) { this.feeType = feeType; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
