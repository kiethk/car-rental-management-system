package com.crms.mvc.enums;

/**
 * Lifecycle of an Issue: OPEN (fee still being agreed) -> RESOLVED (fee settled, ready to collect) or CANCELLED.
 * Stored by name.
 */
public enum IssueStatus {
    OPEN("Đang mở"),
    RESOLVED("Đã chốt"),
    CANCELLED("Đã huỷ");

    private final String label;

    IssueStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
