package com.crms.mvc.util;

import java.time.LocalDate;
import java.util.List;

/**
 * When an inspection (dang kiem) keeps a car off the road. Deliberately simple, whole days only:
 * <ol>
 *   <li>The day of a scheduled inspection is blocked for rentals, so a late return cannot delay the inspection.</li>
 *   <li>A rental that ends after the registration expiry date is only accepted when an inspection is already
 *       scheduled on a day before the rental starts. Without one, the car may expire mid-rental.</li>
 * </ol>
 * Known gap, accepted on purpose: a rental booked before staff scheduled the inspection stays valid; staff then
 * pick another day (the schedule form refuses a day on which the car is already booked).
 */
public final class InspectionRules {
    private InspectionRules() {
    }

    /** Why a single day cannot be rented, for the schedule grid. */
    public enum DayBlock { NONE, INSPECTION_DAY, EXPIRED }

    /**
     * Whether one day is closed to rentals: the day of a scheduled inspection, or a day after the registration
     * expired with no inspection scheduled before it. Same rules as {@link #blocksRental}, for a one-day rental.
     */
    public static DayBlock blockOn(LocalDate registrationExpiry, List<LocalDate> inspectionDays, LocalDate day) {
        if (inspectionDays.contains(day)) {
            return DayBlock.INSPECTION_DAY;
        }
        boolean expired = registrationExpiry != null && day.isAfter(registrationExpiry);
        boolean inspectedBefore = inspectionDays.stream().anyMatch(d -> d.isBefore(day));
        return expired && !inspectedBefore ? DayBlock.EXPIRED : DayBlock.NONE;
    }

    /**
     * @param registrationExpiry the car's registration expiry date, may be null (unknown: not restricted)
     * @param inspectionDays     days of the car's scheduled (not yet done) inspections
     * @param rentalStartDay     day the customer picks the car up
     * @param rentalEndDay       day the customer returns it
     */
    public static boolean blocksRental(LocalDate registrationExpiry, List<LocalDate> inspectionDays,
                                       LocalDate rentalStartDay, LocalDate rentalEndDay) {
        boolean inspectionDuringRental = inspectionDays.stream()
                .anyMatch(day -> !day.isBefore(rentalStartDay) && !day.isAfter(rentalEndDay));
        if (inspectionDuringRental) {
            return true;
        }
        boolean outlivesRegistration = registrationExpiry != null && rentalEndDay.isAfter(registrationExpiry);
        boolean inspectedBeforePickup = inspectionDays.stream().anyMatch(day -> day.isBefore(rentalStartDay));
        return outlivesRegistration && !inspectedBeforePickup;
    }
}
