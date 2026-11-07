package com.crms.mvc.dto;

import java.math.BigDecimal;

public class ReturnReceiptLineDTO {
    private String description;
    private BigDecimal amount;
    private boolean paidEarlier;

    public ReturnReceiptLineDTO() {}

    public ReturnReceiptLineDTO(String description, BigDecimal amount, boolean paidEarlier) {
        this.description = description;
        this.amount = amount;
        this.paidEarlier = paidEarlier;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public boolean isPaidEarlier() { return paidEarlier; }
    public void setPaidEarlier(boolean paidEarlier) { this.paidEarlier = paidEarlier; }
}
