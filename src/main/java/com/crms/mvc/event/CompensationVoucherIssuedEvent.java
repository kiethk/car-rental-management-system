package com.crms.mvc.event;

import java.time.LocalDate;

/** Published inside the cancellation transaction; carries everything the email needs so the listener reads nothing. */
public record CompensationVoucherIssuedEvent(Long bookingId, String customerName, String customerEmail,
                                             String code, int discountPercent, LocalDate validUntil) {
}
