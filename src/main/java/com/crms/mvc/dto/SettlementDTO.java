package com.crms.mvc.dto;

import jakarta.validation.constraints.Size;

public class SettlementDTO {
    private String paymentMethod;

    @Size(max = 20, message = "Bank code must be at most 20 characters")
    private String bankCode;

    @Size(max = 30, message = "Account number must be at most 30 characters")
    private String accountNumber;

    @Size(max = 40, message = "Account holder must be at most 40 characters")
    private String accountHolder;

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getAccountHolder() { return accountHolder; }
    public void setAccountHolder(String accountHolder) { this.accountHolder = accountHolder; }
}
