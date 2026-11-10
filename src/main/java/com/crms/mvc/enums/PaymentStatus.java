package com.crms.mvc.enums;

/** Where a payment stands. Stored by name. */
public enum PaymentStatus {
    PENDING("Chờ thanh toán"),
    PAID("Đã thanh toán"),
    FAILED("Thất bại");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
