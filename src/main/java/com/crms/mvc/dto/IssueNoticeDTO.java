package com.crms.mvc.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** What the customer emails about an Issue need: who to tell, about which booking, and how much, with the names of the evidence photos on file. */
public record IssueNoticeDTO(Long issueId,
                             Long bookingId,
                             String customerName,
                             String customerEmail,
                             String issueTypeLabel,
                             String subject,
                             OffsetDateTime occurredAt,
                             BigDecimal feeAmount,
                             List<String> evidenceFileNames) {
}
