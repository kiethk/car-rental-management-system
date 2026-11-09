package com.crms.mvc.enums;

/** SCHEDULED until staff record the result: PASSED or FAILED. Stored by name. */
public enum InspectionStatus {
    SCHEDULED("Đã lên lịch"),
    PASSED("Đạt"),
    FAILED("Không đạt");

    private final String label;

    InspectionStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
