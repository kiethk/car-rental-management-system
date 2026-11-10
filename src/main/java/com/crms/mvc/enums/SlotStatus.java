package com.crms.mvc.enums;

/** Whether a pick-up slot still takes bookings. Stored by name. */
public enum SlotStatus {
    OPEN("Đang mở"),
    CLOSED("Đã đóng");

    private final String label;

    SlotStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Reads a form or query value such as "sedan" or "SEDAN"; null or unknown gives null. */
    public static SlotStatus from(String value) {
        if (value == null) {
            return null;
        }
        for (SlotStatus item : values()) {
            if (item.name().equalsIgnoreCase(value.trim())) {
                return item;
            }
        }
        return null;
    }
}
