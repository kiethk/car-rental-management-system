package com.crms.mvc.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Published when staff edit an Issue the customer was already told about. Carries what the Issue said before the
 * edit, so the email can show exactly what changed; the current values are read when the email is sent.
 */
public record IssueUpdatedEvent(Long issueId,
                                String previousTypeLabel,
                                String previousSubject,
                                String previousDescription,
                                OffsetDateTime previousOccurredAt,
                                BigDecimal previousFeeAmount) {
}
