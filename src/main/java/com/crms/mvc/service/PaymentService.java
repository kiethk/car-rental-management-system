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
    Payment processSettlement(Long bookingId, BigDecimal lateFee, BigDecimal additionalFine);
    List<Payment> getPaymentsByBooking(Long bookingId);

    Payment createPendingRefund(Long bookingId, BigDecimal amount, String bankCode, String accountNumber, String accountHolder);
    Payment confirmRefundCompleted(Long paymentId, Long employeeId, String transactionNo);
    Optional<Payment> getPendingRefund(Long bookingId);
    List<Payment> getPendingRefunds();
    long countPendingRefunds();
    List<Long> getPendingRefundBookingIds();
}
