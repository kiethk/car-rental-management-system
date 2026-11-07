package com.crms.mvc.config;

import java.time.LocalDateTime;

/**
 * When the depot hands over and takes back cars: pickup and return both happen on a whole hour between
 * {@code open} and {@code close} inclusive (crms.opening-hours.* in application.properties).
 */
public record OpeningHours(int open, int close) {

    public static final OpeningHours DEFAULT = new OpeningHours(6, 22);

    public boolean contains(LocalDateTime time) {
        int hour = time.getHour();
        return hour >= open && hour <= close && (hour < close || time.getMinute() == 0);
    }

    /** "06:00 - 22:00" */
    public String getLabel() {
        return String.format("%02d:00 - %02d:00", open, close);
    }

    public int getOpen() { return open; }
    public int getClose() { return close; }
}
