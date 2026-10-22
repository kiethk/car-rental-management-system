package com.crms.mvc.service;

import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Payment;
import java.math.BigDecimal;
import java.util.List;

public interface PaymentService {
    Payment processDeposit(Long bookingId);
    Payment processSettlement(Long bookingId, BigDecimal lateFee, BigDecimal additionalFine);
    List<Payment> getPaymentsByBooking(Long bookingId);
}

