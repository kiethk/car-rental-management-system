package com.crms.mvc.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** What the customer emails about an Issue need: who to tell, about which booking, and how much. */
public record IssueNoticeDTO(Long issueId,
                             Long bookingId,
                             String customerName,
                             String customerEmail,
                             String issueTypeLabel,
                             String subject,
                             OffsetDateTime occurredAt,
                             BigDecimal feeAmount) {
}
