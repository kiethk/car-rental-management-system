package com.crms.mvc.service;

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
