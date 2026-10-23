package com.crms.mvc.service.impl;

import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Payment;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.PaymentRepository;
import com.crms.mvc.service.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    public PaymentServiceImpl(PaymentRepository paymentRepository, BookingRepository bookingRepository) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public Payment processDeposit(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Booking not found: " + bookingId));

        if ("confirmed".equalsIgnoreCase(booking.getStatus())) {
            return paymentRepository.findByBookingIdAndPaymentType(bookingId, "DEPOSIT").orElse(null);
        }

        if (!"pending_payment".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Booking is not in pending_payment status");
        }

        Payment payment = paymentRepository.findByBookingIdAndPaymentType(bookingId, "DEPOSIT")
                .orElseGet(() -> {
                    Payment p = new Payment();
                    p.setBooking(booking);
                    p.setPaymentType("DEPOSIT");
                    p.setAmount(booking.getDepositAmount());
                    return p;
                });

        payment.setStatus("paid");
        payment.setPaidAt(OffsetDateTime.now());
        Payment saved = paymentRepository.save(payment);

        booking.setStatus("confirmed");
        bookingRepository.save(booking);

        return saved;
    }

    @Override
    public Payment processSettlement(Long bookingId, BigDecimal lateFee, BigDecimal additionalFine) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Booking not found: " + bookingId));

        if (lateFee != null && lateFee.compareTo(BigDecimal.ZERO) > 0) {
            booking.setLateFee(lateFee);
            booking.setTotalPrice(booking.getTotalPrice().add(lateFee));
        }

        BigDecimal extra = (additionalFine != null) ? additionalFine : BigDecimal.ZERO;
        BigDecimal remaining = booking.getTotalPrice().subtract(booking.getDepositAmount());
        if (remaining.compareTo(BigDecimal.ZERO) < 0) remaining = BigDecimal.ZERO;
        BigDecimal finalPaymentAmount = remaining.add(extra);

        Payment finalPayment = new Payment();
        finalPayment.setBooking(booking);
        finalPayment.setPaymentType("FINAL");
        finalPayment.setAmount(finalPaymentAmount);
        finalPayment.setStatus("paid");
        finalPayment.setPaidAt(OffsetDateTime.now());
        paymentRepository.save(finalPayment);

        booking.setStatus("completed");
        bookingRepository.save(booking);

        return finalPayment;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByBooking(Long bookingId) {
        return paymentRepository.findByBookingIdOrderByPaidAtAsc(bookingId);
    }
}
