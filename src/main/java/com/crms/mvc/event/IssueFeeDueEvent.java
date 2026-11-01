package com.crms.mvc.event;

/**
 * Published when an issue is resolved after the booking was already settled: the customer now owes its fee
 * and has to be told. Resolving an issue before the car is returned needs no event (the fee goes into settlement).
 */
public record IssueFeeDueEvent(Long issueId) {
}
