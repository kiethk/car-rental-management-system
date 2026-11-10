package com.crms.mvc.enums;

/** How a promotion takes money off: a percentage or a fixed amount. Stored by name. */
public enum DiscountType {
    PERCENT("Phần trăm"),
    FIXED("Số tiền cố định");

    private final String label;

    DiscountType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Reads a form or query value such as "sedan" or "SEDAN"; null or unknown gives null. */
    public static DiscountType from(String value) {
        if (value == null) {
            return null;
        }
        for (DiscountType item : values()) {
            if (item.name().equalsIgnoreCase(value.trim())) {
                return item;
            }
        }
        return null;
    }
}
