package com.crms.mvc.service;

import com.crms.mvc.dto.BookingCancelDTO;
import com.crms.mvc.dto.BookingCreateDTO;
import com.crms.mvc.entity.Booking;

import java.time.LocalDateTime;

/**
 * The life of a booking: create it, cancel it, change its car before handover. Reading bookings is
 * {@link BookingQueryService}; which cars are free is {@link VehicleAvailabilityService}.
 */
public interface BookingService {
    /** clientIp is stored as clickwrap evidence on the contract. */
    Booking createBooking(Long customerId, BookingCreateDTO dto, String clientIp);

    /**
     * Checks a requested rental window: both times fall on a whole hour (minute :00), pickup is not in the past
     * and return is after pickup.
     * Throws IllegalArgumentException with a customer-facing message instead of silently adjusting the times.
     */
    void validateRentalSchedule(LocalDateTime start, LocalDateTime end);

    /** Cancels a booking whose deposit was never paid (the customer walked away or the payment window ran out). */
    void cancelUnpaidBooking(Long bookingId, Long customerId);

    /**
     * Cancels a booking for its customer. A paid booking cancelled before its free-cancellation deadline gets its
     * deposit back (a pending refund to the bank account in the dto); the result says whether a refund was created.
     */
    boolean cancelBooking(Long bookingId, Long customerId, BookingCancelDTO dto);

    /**
     * Đổi xe before handover: only to another car of the booked type, so the contract is still fulfilled.
     * Never to another type, which would let a late return be used to get a better car at a cheaper price.
     */
    void assignVehicle(Long bookingId, Long vehicleId);

    /**
     * No car of the booked type is free for this booking's rental window: the rental company cannot fulfil the
     * contract, so cancel the confirmed booking and hand back 100% of the deposit at the counter (refundMethod CASH
     * or BANK_TRANSFER), regardless of the free-cancellation deadline. Refused while a same-type car is free.
     */
    void cancelForUnavailableCar(Long bookingId, String refundMethod);

    /** Cancels bookings whose deposit was not paid within the payment window; runs on a schedule. */
    void cancelExpiredPendingBookings();
}
