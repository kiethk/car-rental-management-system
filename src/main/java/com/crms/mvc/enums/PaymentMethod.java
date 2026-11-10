package com.crms.mvc.enums;

import com.crms.mvc.enums.PaymentMethod;
/** How the money moved. Stored by name. */
public enum PaymentMethod {
    VNPAY("VNPay"),
    BANK_TRANSFER("Chuyển khoản"),
    CASH("Tiền mặt");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Reads a form or query value such as "sedan" or "SEDAN"; null or unknown gives null. */
    public static PaymentMethod from(String value) {
        if (value == null) {
            return null;
        }
        for (PaymentMethod item : values()) {
            if (item.name().equalsIgnoreCase(value.trim())) {
                return item;
            }
        }
        return null;
    }
}
