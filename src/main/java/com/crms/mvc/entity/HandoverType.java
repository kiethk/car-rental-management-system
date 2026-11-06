package com.crms.mvc.entity;

/** Which side of the rental a Handover records. */
public enum HandoverType {
    CHECK_IN("Nhận xe"),
    CHECK_OUT("Trả xe");

    private final String label;

    HandoverType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
