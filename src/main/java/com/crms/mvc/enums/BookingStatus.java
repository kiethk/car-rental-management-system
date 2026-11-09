package com.crms.mvc.enums;

import com.crms.mvc.enums.BookingStatus;
/** Where a booking is: waiting for the deposit, confirmed, on the road, finished or cancelled. Stored by name. */
public enum BookingStatus {
    PENDING_PAYMENT("Chờ đặt cọc"),
    CONFIRMED("Đã xác nhận"),
    ACTIVE("Đang thuê"),
    COMPLETED("Hoàn tất"),
    CANCELLED("Đã huỷ");

    private final String label;

    BookingStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Reads a form or query value such as "sedan" or "SEDAN"; null or unknown gives null. */
    public static BookingStatus from(String value) {
        if (value == null) {
            return null;
        }
        for (BookingStatus item : values()) {
            if (item.name().equalsIgnoreCase(value.trim())) {
                return item;
            }
        }
        return null;
    }
}
