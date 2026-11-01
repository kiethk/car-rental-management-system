package com.crms.mvc.entity;

/**
 * Lifecycle of an Issue: OPEN (fee still being agreed) -> RESOLVED (fee settled, ready to collect) or CANCELLED.
 * Stored lowercase, like the booking and vehicle statuses.
 */
public enum IssueStatus {
    OPEN("open", "Đang mở"),
    RESOLVED("resolved", "Đã chốt"),
    CANCELLED("cancelled", "Đã huỷ");

    private final String dbValue;
    private final String label;

    IssueStatus(String dbValue, String label) {
        this.dbValue = dbValue;
        this.label = label;
    }

    public String dbValue() {
        return dbValue;
    }

    public String getLabel() {
        return label;
    }

    public static IssueStatus fromDbValue(String value) {
        for (IssueStatus s : values()) {
            if (s.dbValue.equals(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown issue status: " + value);
    }
}
