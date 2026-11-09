package com.crms.mvc.util;

import com.crms.mvc.dto.RentalWindow;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

/**
 * Reads the rental window out of the query parameters of the search and checkout pages. The pages have used several
 * spellings over time, so all are accepted: {@code startDate}/{@code endDate} (or the older single {@code date}) with
 * {@code startTime}/{@code endTime} as "HH:mm", or full "yyyy-MM-ddTHH:mm" values in the time parameters. Whatever is
 * missing or unreadable falls back to a default (tomorrow, 09:00 to 18:00); validation of the result is the booking
 * service's job, so a nonsense value never reaches the customer as an error page.
 */
public final class RentalWindowParser {
    private static final LocalTime DEFAULT_START = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_END = LocalTime.of(18, 0);

    private RentalWindowParser() {
    }

    public static RentalWindow parse(String startDate, String endDate, String date, String startTime, String endTime) {
        LocalDate defaultDay = LocalDate.now(DateTimeUtils.VIETNAM_ZONE).plusDays(1);

        LocalDate firstDay = date(startDate).or(() -> date(date)).orElse(defaultDay);
        // Without an end date the rental ends the day it starts, unless only the legacy "date" was given.
        LocalDate lastDay = date(endDate).or(() -> startDate != null && !startDate.isBlank() ? Optional.of(firstDay) : date(date))
                .orElse(firstDay);

        LocalDateTime start = dateTime(startTime).orElseGet(() -> firstDay.atTime(timeOfDay(startTime).orElse(DEFAULT_START)));
        LocalDateTime end = dateTime(endTime).orElseGet(() -> lastDay.atTime(timeOfDay(endTime).orElse(DEFAULT_END)));
        return new RentalWindow(start, end);
    }

    private static Optional<LocalDate> date(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(text.trim()));
        } catch (DateTimeException unreadable) {
            return Optional.empty();
        }
    }

    /** A full date-time value, recognised by its "T"; anything else is a time of day. */
    private static Optional<LocalDateTime> dateTime(String text) {
        if (text == null || !text.contains("T")) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDateTime.parse(text));
        } catch (DateTimeException unreadable) {
            return Optional.empty();
        }
    }

    private static Optional<LocalTime> timeOfDay(String text) {
        if (text == null || text.isBlank() || text.contains("T")) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalTime.parse(text.trim()));
        } catch (DateTimeException unreadable) {
            return Optional.empty();
        }
    }
}
