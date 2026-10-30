package com.crms.mvc.service;

import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Payment;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface PaymentService {
    String createDepositPaymentUrl(Booking booking, HttpServletRequest request);
    boolean verifyPaymentSignature(Map<String, String> vnpParams);

    Payment processDeposit(Long bookingId);
    Payment processDeposit(Long bookingId, String transactionNo, String paymentMethod);
    Payment processSettlement(Long bookingId, BigDecimal lateFee, BigDecimal additionalFine);
    List<Payment> getPaymentsByBooking(Long bookingId);
}
