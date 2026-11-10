package com.crms.mvc.enums;

/** What powers a vehicle type. Stored by name. */
public enum FuelType {
    GASOLINE("Xăng"),
    DIESEL("Dầu diesel"),
    ELECTRIC("Điện"),
    HYBRID("Hybrid");

    private final String label;

    FuelType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Reads a form or query value such as "sedan" or "SEDAN"; null or unknown gives null. */
    public static FuelType from(String value) {
        if (value == null) {
            return null;
        }
        for (FuelType item : values()) {
            if (item.name().equalsIgnoreCase(value.trim())) {
                return item;
            }
        }
        return null;
    }
}
