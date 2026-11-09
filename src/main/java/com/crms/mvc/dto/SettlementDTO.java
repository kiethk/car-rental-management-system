package com.crms.mvc.dto;

/** How the customer paid the rest of the rent and the unpaid fees at the counter (cash or bank transfer). */
public class SettlementDTO {
    private String paymentMethod;

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
}
