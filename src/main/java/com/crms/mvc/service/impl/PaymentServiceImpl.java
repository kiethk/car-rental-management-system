package com.crms.mvc.service.impl;

import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Payment;
import com.crms.mvc.event.DepositConfirmedEvent;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.PaymentRepository;
import com.crms.mvc.service.BookingFeeService;
import com.crms.mvc.service.PaymentService;
import org.springframework.context.ApplicationEventPublisher;
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
    private final BookingFeeService bookingFeeService;
    private final ApplicationEventPublisher eventPublisher;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              BookingRepository bookingRepository,
                              BookingFeeService bookingFeeService,
                              ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.bookingFeeService = bookingFeeService;
        this.eventPublisher = eventPublisher;
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

        payment.setTransactionCode("TXN-DEP-" + bookingId + "-" + System.currentTimeMillis());
        payment.setPaymentMethod("VIETQR");
        payment.setStatus("paid");
        payment.setPaidAt(OffsetDateTime.now());
        Payment saved = paymentRepository.save(payment);

        booking.setStatus("confirmed");
        bookingRepository.save(booking);

        // The contract PDF is issued by ContractIssuingListener after this transaction commits.
        eventPublisher.publishEvent(new DepositConfirmedEvent(bookingId));

        return saved;
    }

    @Override
    public Payment processSettlement(Long bookingId, BigDecimal lateFee, BigDecimal additionalFine) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Booking not found: " + bookingId));

        if (lateFee != null && lateFee.compareTo(BigDecimal.ZERO) > 0) {
            booking.setLateFee(lateFee);
            booking.setTotalPrice(booking.getTotalPrice().add(lateFee));
            bookingFeeService.recordFee(bookingId, "LATE_FEE", lateFee, "Late return fee beyond agreed schedule", null);
        }

        BigDecimal extra = BigDecimal.ZERO;
        if (additionalFine != null && additionalFine.compareTo(BigDecimal.ZERO) > 0) {
            extra = additionalFine;
            bookingFeeService.recordFee(bookingId, "DAMAGE_COMPENSATION", additionalFine, "Incidental damage / compensation fee", null);
        }

        BigDecimal remaining = booking.getTotalPrice().subtract(booking.getDepositAmount());
        if (remaining.compareTo(BigDecimal.ZERO) < 0) remaining = BigDecimal.ZERO;
        BigDecimal finalPaymentAmount = remaining.add(extra);

        Payment finalPayment = new Payment();
        finalPayment.setBooking(booking);
        finalPayment.setPaymentType("FINAL");
        finalPayment.setTransactionCode("TXN-FIN-" + bookingId + "-" + System.currentTimeMillis());
        finalPayment.setPaymentMethod("VIETQR");
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
