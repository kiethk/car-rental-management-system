package com.crms.mvc.enums;

/** Body style of a vehicle type. Stored by name. */
public enum VehicleCategory {
    SEDAN("Sedan"),
    SUV("SUV"),
    HATCHBACK("Hatchback"),
    MINIVAN("Minivan"),
    PICKUP("Bán tải");

    private final String label;

    VehicleCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Reads a form or query value such as "sedan" or "SEDAN"; null or unknown gives null. */
    public static VehicleCategory from(String value) {
        if (value == null) {
            return null;
        }
        for (VehicleCategory item : values()) {
            if (item.name().equalsIgnoreCase(value.trim())) {
                return item;
            }
        }
        return null;
    }
}
