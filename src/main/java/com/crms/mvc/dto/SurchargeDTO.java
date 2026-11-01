package com.crms.mvc.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class SurchargeDTO {
    private String feeType;

    @DecimalMin(value = "0", message = "Surcharge cannot be negative")
    @DecimalMax(value = "999999999999", message = "Surcharge is too large")
    private BigDecimal amount;

    @Size(max = 200, message = "Surcharge description must be at most 200 characters")
    private String description;

    public String getFeeType() { return feeType; }
    public void setFeeType(String feeType) { this.feeType = feeType; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
