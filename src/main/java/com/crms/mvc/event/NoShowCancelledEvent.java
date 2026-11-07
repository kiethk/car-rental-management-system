package com.crms.mvc.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Published inside the cancellation transaction; carries everything the email needs so the listener reads nothing. */
public record NoShowCancelledEvent(Long bookingId, String customerName, String customerEmail, String vehicleName,
                                   OffsetDateTime pickupTime, OffsetDateTime cancelledAt, BigDecimal depositAmount,
                                   int pickupWaitHours) {
}
