package com.crms.mvc.dto;

import java.io.Serializable;

public class BookingCancelDTO implements Serializable {
    private String bankCode;
    private String accountNumber;
    private String accountHolder;
    private String reason;

    public BookingCancelDTO() {
    }

    public BookingCancelDTO(String bankCode, String accountNumber, String accountHolder, String reason) {
        this.bankCode = bankCode;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.reason = reason;
    }

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountHolder() {
        return accountHolder;
    }

    public void setAccountHolder(String accountHolder) {
        this.accountHolder = accountHolder;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

