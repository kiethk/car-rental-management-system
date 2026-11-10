package com.crms.mvc.enums;

/** Gearbox of a vehicle type. Stored by name. */
public enum Transmission {
    AUTOMATIC("Số tự động"),
    MANUAL("Số sàn");

    private final String label;

    Transmission(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Reads a form or query value such as "sedan" or "SEDAN"; null or unknown gives null. */
    public static Transmission from(String value) {
        if (value == null) {
            return null;
        }
        for (Transmission item : values()) {
            if (item.name().equalsIgnoreCase(value.trim())) {
                return item;
            }
        }
        return null;
    }
}
