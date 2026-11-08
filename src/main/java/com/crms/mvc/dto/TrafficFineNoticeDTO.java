package com.crms.mvc.dto;

import java.time.OffsetDateTime;

/** A recorded traffic fine with the rental facts staff and the customer need to place it. */
public record TrafficFineNoticeDTO(
        Long id,
        Long bookingId,
        String customerName,
        String customerEmail,
        String plate,
        OffsetDateTime violationAt,
        OffsetDateTime recordedAt,
        String recordedByName,
        OffsetDateTime handoverAt,
        OffsetDateTime returnedAt,
        String contractNumber) {
}
