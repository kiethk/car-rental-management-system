package com.crms.mvc.enums;

public enum FeeType {
    ISSUE_COMPENSATION("Bồi thường sự cố"),
    LATE_FEE("Phí trả xe trễ"),
    RENTAL("Tiền thuê xe"),
    DAMAGE("Hư hỏng xe"),
    OTHER("Phí khác");

    private final String label;

    FeeType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static FeeType from(String value) {
        if (value == null || value.isBlank()) {
            return OTHER;
        }
        for (FeeType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        return OTHER;
    }
}
