package com.crms.mvc.dto;

/** A rental length in whole hours, split into days and hours the way the pages show it ("2d 12h"). */
public record RentalDuration(long hours) {

    public long days() {
        return hours / 24;
    }

    public long remainingHours() {
        return hours % 24;
    }

    /** "9h", "1d 3h", "2d " for an exact number of days (the trailing space is how the pages have always shown it). */
    public String display() {
        return (days() > 0 ? (days() + "d ") : "")
                + (remainingHours() > 0 ? (remainingHours() + "h") : (days() == 0 ? "1h" : ""));
    }
}
