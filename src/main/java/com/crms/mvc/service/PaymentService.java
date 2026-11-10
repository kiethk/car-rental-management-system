package com.crms.mvc.service;

import com.crms.mvc.enums.PaymentMethod;
import com.crms.mvc.dto.DepositSessionDTO;
import com.crms.mvc.entity.Payment;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.NoSuchElementException;

public interface PaymentService {
    /** Owner, status, amount and age of a booking's deposit; empty when the booking does not exist. */
    Optional<DepositSessionDTO> findDepositSession(Long bookingId);

    /** True when the query the payment provider sent back to the return address carries a valid signature. */
    boolean isValidGatewayReturn(Map<String, String> queryParams);

    /** The gateway URL the customer is sent to for the deposit of this booking. */
    String createDepositPaymentUrl(Long bookingId, String clientIp);

    /** Records the deposit as paid and confirms the booking; repeating it for a confirmed booking changes nothing. */
    Payment confirmDeposit(Long bookingId, String transactionNo, PaymentMethod paymentMethod);

    /**
     * True while the customer has an unexpired VNPay session for Issue fees on this booking.
     * An expired session is released (marked failed, its fees freed) as a side effect.
     */
    boolean hasLiveIssuePaymentSession(Long bookingId);

    /**
     * Starts paying every outstanding issue fee of a rental in progress in one VNPay payment; the amount is the sum
     * the system computes, never typed by the customer. Returns the VNPay URL to redirect to. A payment left
     * pending by an earlier abandoned attempt is cancelled first, so its fees are not stuck.
     *
     * @throws NoSuchElementException when the booking is not the customer's
     * @throws IllegalStateException when the rental is not in progress (already settled or not picked up) or nothing is owed
     */
    String createIssuePaymentUrl(Long bookingId, Long customerId, String clientIp);

    /** The gateway reported success: marks the issue payment paid. Repeating it changes nothing. */
    Payment confirmIssuePayment(Long paymentId, String transactionNo, long amountInCents);

    /** The gateway reported failure or cancellation: marks the payment failed and gives its fees back as owed. */
    Payment failIssuePayment(Long paymentId);

    /** Settlement payment: the unpaid fees only. */
    Payment recordFinalPayment(Long bookingId, BigDecimal amount, PaymentMethod paymentMethod);

    /** The rest of the rent (total minus deposit), collected by staff at handover in cash or by bank transfer. */
    Payment recordRentalBalancePayment(Long bookingId, BigDecimal amount, PaymentMethod paymentMethod);
    List<Payment> getPaymentsByBooking(Long bookingId);

    Payment createPendingRefund(Long bookingId, BigDecimal amount, String bankCode, String accountNumber, String accountHolder);
    Payment confirmRefundCompleted(Long paymentId, Long employeeId, String transactionNo);

    /**
     * Money handed back to a customer who is at the counter: recorded as an already paid refund, in cash or by
     * bank transfer, and the customer is notified like any completed refund.
     */
    Payment recordCounterRefund(Long bookingId, BigDecimal amount, PaymentMethod paymentMethod);
    Optional<Payment> getPendingRefund(Long bookingId);
    List<Payment> getPendingRefunds();
    long countPendingRefunds();
    List<Long> getPendingRefundBookingIds();
}
