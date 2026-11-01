package com.crms.mvc.dto;

import java.math.BigDecimal;

/** The three numbers on top of the admin issue list. */
public record IssueStatsDTO(long open, long awaitingPayment, BigDecimal collectedThisMonth) {
}
