package com.crms.mvc.dto;

import com.crms.mvc.util.DateTimeUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;

/** The hours a customer wants a car for, as typed in Vietnam time (the form's date-time inputs carry no zone). */
public record RentalWindow(LocalDateTime start, LocalDateTime end) {

    public LocalDate startDate() {
        return start.toLocalDate();
    }

    public LocalDate endDate() {
        return end.toLocalDate();
    }

    public LocalTime startTime() {
        return start.toLocalTime();
    }

    public LocalTime endTime() {
        return end.toLocalTime();
    }

    public OffsetDateTime startAt() {
        return start.atOffset(DateTimeUtils.VIETNAM_OFFSET);
    }

    public OffsetDateTime endAt() {
        return end.atOffset(DateTimeUtils.VIETNAM_OFFSET);
    }

    /** Whole hours between the two moments, at least one. */
    public RentalDuration duration() {
        return new RentalDuration(Math.max(1, Duration.between(start, end).toHours()));
    }
}
