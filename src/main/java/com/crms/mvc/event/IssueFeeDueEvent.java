package com.crms.mvc.event;

/**
 * Published when an Issue is recorded: the customer now owes its fee and has to be told.
 */
public record IssueFeeDueEvent(Long issueId) {
}
