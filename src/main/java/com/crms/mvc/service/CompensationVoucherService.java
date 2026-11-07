package com.crms.mvc.service;

import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Promotion;

import java.util.Optional;

/**
 * Voucher bù: when a booking is cancelled because no car of the booked type was left, the customer gets a personal,
 * single-use voucher for a later rental, at the percentage and validity of the contract they accepted. It is a discount, never cash, so a staged late return cannot be turned
 * into money.
 */
public interface CompensationVoucherService {

    /** Creates the voucher for this booking and emails it to the customer once the cancellation is committed. */
    Promotion issueFor(Booking booking);

    /** The voucher issued for this booking, if it was cancelled for lack of a car. */
    Optional<Promotion> findForBooking(Long bookingId);
}
