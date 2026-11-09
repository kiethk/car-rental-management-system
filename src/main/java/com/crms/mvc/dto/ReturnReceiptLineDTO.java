package com.crms.mvc.dto;

import java.math.BigDecimal;

/** One charge on the return receipt; {@code paidEarlier} marks fees the customer already paid before settlement. */
public record ReturnReceiptLineDTO(String description, BigDecimal amount, boolean paidEarlier) {
}
