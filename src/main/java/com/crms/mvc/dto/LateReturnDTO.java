package com.crms.mvc.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Where an overdue rental stands right now: how late the car is, how many full late hours are already charged,
 * the late fee so far, and when the next hour starts to count (see GLOSSARY "Ân hạn trả xe").
 *
 * @param lateMinutes   minutes since the scheduled return time
 * @param chargedHours  full late hours charged so far; 0 while still inside the first hour
 * @param currentFee    late fee if the car were returned now
 * @param nextChargeAt  when the next late hour starts to be charged
 */
public record LateReturnDTO(long lateMinutes, long chargedHours, BigDecimal currentFee, OffsetDateTime nextChargeAt)
        implements Serializable {

    /** Still inside the first late hour: overdue, but nothing charged yet. */
    public boolean isInGrace() {
        return chargedHours == 0;
    }

    /** "2 giờ 15 phút", "45 phút". */
    public String getLateText() {
        long hours = lateMinutes / 60;
        long minutes = lateMinutes % 60;
        if (hours == 0) return minutes + " phút";
        return minutes == 0 ? hours + " giờ" : hours + " giờ " + minutes + " phút";
    }
}
