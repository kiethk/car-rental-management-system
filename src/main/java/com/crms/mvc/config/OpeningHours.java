package com.crms.mvc.config;

import java.time.LocalDateTime;

/**
 * When the depot hands over and takes back cars: pickup and return both happen on a whole hour between
 * {@code open} and {@code close} inclusive (crms.opening-hours.* in application.properties). The depot now
 * works around the clock, so the default allows every hour of the day.
 */
public record OpeningHours(int open, int close) {

    public static final OpeningHours DEFAULT = new OpeningHours(0, 23);

    public boolean contains(LocalDateTime time) {
        int hour = time.getHour();
        return hour >= open && hour <= close && (hour < close || time.getMinute() == 0);
    }

    /** Every hour of the day is allowed. */
    public boolean isAllDay() {
        return open <= 0 && close >= 23;
    }

    /** "24/24", or "06:00 - 22:00" when the depot is configured with shorter hours. */
    public String getLabel() {
        return isAllDay() ? "24/24" : String.format("%02d:00 - %02d:00", open, close);
    }

    public int getOpen() { return open; }
    public int getClose() { return close; }
}
