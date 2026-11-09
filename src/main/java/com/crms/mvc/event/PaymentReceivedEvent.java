package com.crms.mvc.event;

import com.crms.mvc.util.EnumTokens;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Payment;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Published when a customer's payment is recorded after the deposit: the rest of the rent at handover, the fees
 * collected at settlement, or Issue fees paid on their own. Carries everything the thank-you email needs.
 */
public record PaymentReceivedEvent(Long bookingId, String customerName, String customerEmail, String vehicleName,
                                   BigDecimal amount, String paymentMethod, OffsetDateTime paidAt, Kind kind) {

    public enum Kind {
        /** The remaining part of the rent, collected when the car is handed over. */
        RENTAL_BALANCE,
        /** Late fee, Issue fees and other charges collected at settlement; the booking is closed afterwards. */
        SETTLEMENT,
        /** Issue fees paid before settlement, online or at the counter. */
        ISSUE_FEES
    }

    public static PaymentReceivedEvent of(Booking booking, Payment payment, Kind kind) {
        String vehicleName = booking.getSlot() != null && booking.getSlot().getVehicleType() != null
                ? booking.getSlot().getVehicleType().getDisplayName() : null;
        return new PaymentReceivedEvent(booking.getId(),
                booking.getCustomer() != null ? booking.getCustomer().getName() : null,
                booking.getCustomer() != null ? booking.getCustomer().getEmail() : null,
                vehicleName, payment.getAmount(), EnumTokens.name(payment.getPaymentMethod()), payment.getPaidAt(), kind);
    }
}
