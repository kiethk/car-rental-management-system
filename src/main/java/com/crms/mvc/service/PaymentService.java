package com.crms.mvc.service;

import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Payment;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PaymentService {
    String createDepositPaymentUrl(Booking booking, String clientIp);
    boolean verifyPaymentSignature(Map<String, String> queryParams);

    Payment processDeposit(Long bookingId);
    Payment processDeposit(Long bookingId, String transactionNo, String paymentMethod);
    /**
     * Final settlement when the car comes back: rent, late fee and the fees of resolved issues, set against the
     * deposit, as a FINAL payment (customer owes) or a REFUND (deposit left over). Every fee collected is tied
     * to the resulting payment.
     */
    Payment processSettlement(Long bookingId, BigDecimal lateFee);

    /**
     * Starts paying every outstanding issue fee of a completed booking in one VNPay payment; the amount is the sum
     * the system computes, never typed by the customer. Returns the VNPay URL to redirect to. A payment left
     * pending by an earlier abandoned attempt is cancelled first, so its fees are not stuck.
     *
     * @throws java.util.NoSuchElementException when the booking is not the customer's
     * @throws IllegalStateException when the booking is not completed or nothing is owed
     */
    String createIssuePaymentUrl(Long bookingId, Long customerId, String clientIp);

    /** The gateway reported success: marks the issue payment paid. Repeating it changes nothing. */
    Payment confirmIssuePayment(Long paymentId, String transactionNo, long amountInCents);

    /** The gateway reported failure or cancellation: marks the payment failed and gives its fees back as owed. */
    Payment failIssuePayment(Long paymentId);
    List<Payment> getPaymentsByBooking(Long bookingId);

    Payment createPendingRefund(Long bookingId, BigDecimal amount, String bankCode, String accountNumber, String accountHolder);
    Payment confirmRefundCompleted(Long paymentId, Long employeeId, String transactionNo);
    Optional<Payment> getPendingRefund(Long bookingId);
    List<Payment> getPendingRefunds();
    long countPendingRefunds();
    List<Long> getPendingRefundBookingIds();
}
