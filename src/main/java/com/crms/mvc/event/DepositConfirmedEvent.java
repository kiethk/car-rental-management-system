package com.crms.mvc.event;

/**
 * Published by PaymentService when a booking deposit has been recorded as paid.
 */
public record DepositConfirmedEvent(Long bookingId) {
}
