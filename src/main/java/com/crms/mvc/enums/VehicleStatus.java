package com.crms.mvc.enums;

import com.crms.mvc.enums.VehicleStatus;
/** Where a vehicle is: ready to rent, out with a customer, in the workshop, or retired for good. Stored by name. */
public enum VehicleStatus {
    AVAILABLE("Sẵn sàng"),
    RENTED("Đang thuê"),
    MAINTENANCE("Bảo dưỡng"),
    RETIRED("Ngừng khai thác");

    private final String label;

    VehicleStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Reads a form or query value such as "sedan" or "SEDAN"; null or unknown gives null. */
    public static VehicleStatus from(String value) {
        if (value == null) {
            return null;
        }
        for (VehicleStatus item : values()) {
            if (item.name().equalsIgnoreCase(value.trim())) {
                return item;
            }
        }
        return null;
    }
}
