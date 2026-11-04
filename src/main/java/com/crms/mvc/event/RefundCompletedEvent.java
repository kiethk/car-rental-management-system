package com.crms.mvc.event;

/**
 * Published when a pending deposit refund has been marked as completed by staff.
 */
public record RefundCompletedEvent(Long bookingId, Long paymentId) {
}

