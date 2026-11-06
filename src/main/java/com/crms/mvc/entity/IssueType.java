package com.crms.mvc.entity;

/**
 * What kind of incident an Issue records. Only COLLISION and DAMAGE send the vehicle to maintenance on return.
 */
public enum IssueType {
    COLLISION("Va chạm"),
    DAMAGE("Hư hỏng"),
    TRAFFIC_FINE("Phạt nguội"),
    OTHER("Khác");

    private final String label;

    IssueType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** True when the vehicle may be physically damaged and must be inspected before renting again. */
    public boolean damagesVehicle() {
        return this == COLLISION || this == DAMAGE;
    }
}
