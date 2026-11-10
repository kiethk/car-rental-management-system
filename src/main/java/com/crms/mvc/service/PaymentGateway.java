package com.crms.mvc.service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * The online payment provider's side of a payment and nothing else: it builds the signed address the customer is
 * sent to and checks the signature of the answer. What the money means for a booking (deposit, fees, refunds)
 * lives in {@link PaymentService}.
 */
public interface PaymentGateway {

    /** Signs and assembles the address the customer is redirected to; the amount is in dong. */
    String createPaymentUrl(BigDecimal amount, String txnRef, String orderInfo, String clientIp);

    /** True when the query the provider sent back to the return address carries a valid signature. */
    boolean verifySignature(Map<String, String> queryParams);
}
