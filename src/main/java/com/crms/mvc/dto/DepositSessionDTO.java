package com.crms.mvc.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** What the payment pages need to know about a booking's deposit: whose it is, where it stands and how much it is. */
public record DepositSessionDTO(Long bookingId,
                                Long customerId,
                                String status,
                                BigDecimal depositAmount,
                                OffsetDateTime createdAt) {
}
