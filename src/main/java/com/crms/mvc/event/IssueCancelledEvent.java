package com.crms.mvc.event;

/**
 * Published when an issue is cancelled by staff/admin, releasing the customer from liability.
 */
public record IssueCancelledEvent(Long issueId, String cancelReason) {
}
