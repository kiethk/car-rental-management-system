package com.crms.mvc.notification;

import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Payment;
import com.crms.mvc.event.RefundCompletedEvent;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.PaymentRepository;
import com.crms.mvc.util.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Listens for completed refund events and notifies the customer via email.
 */
@Component
public class RefundNotificationListener {
    private static final Logger log = LoggerFactory.getLogger(RefundNotificationListener.class);
    private static final String TEMPLATE = "mail/refund-completed";

    private final CustomerMailer mailer;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    public RefundNotificationListener(CustomerMailer mailer,
                                      BookingRepository bookingRepository,
                                      PaymentRepository paymentRepository) {
        this.mailer = mailer;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRefundCompleted(RefundCompletedEvent event) {
        log.info("Handling RefundCompletedEvent for booking #{} payment #{}", event.bookingId(), event.paymentId());
        try {
            if (!mailer.isConfigured()) {
                log.info("Email service not configured; skipping refund notification email for booking #{}", event.bookingId());
                return;
            }

            Booking booking = bookingRepository.findById(event.bookingId()).orElse(null);
            Payment payment = paymentRepository.findById(event.paymentId()).orElse(null);

            if (booking == null || payment == null || booking.getCustomer() == null || booking.getCustomer().getEmail() == null) {
                log.warn("Cannot send refund email: missing booking, payment, or customer email for booking #{}", event.bookingId());
                return;
            }

            String raw = payment.getTransactionCode() != null ? payment.getTransactionCode() : "";
            String bankCode = "";
            String accountNumber = "";
            String accountHolder = "";
            if (raw.contains(":")) {
                String[] parts = raw.split(":", 2);
                bankCode = parts[0].trim();
                String rest = parts[1].trim();
                if (rest.contains("-")) {
                    String[] accParts = rest.split("-", 2);
                    accountNumber = accParts[0].trim();
                    accountHolder = accParts[1].trim();
                } else {
                    accountNumber = rest;
                }
            }

            Map<String, Object> vars = new HashMap<>();
            vars.put("customerName", booking.getCustomer().getName());
            vars.put("bookingId", booking.getId());
            vars.put("refundAmount", payment.getAmount());
            vars.put("bankCode", bankCode);
            vars.put("accountNumber", accountNumber);
            vars.put("accountHolder", accountHolder);
            vars.put("paidAt", DateTimeUtils.toVietnamTime(payment.getPaidAt()));
            vars.put("vehicleName", booking.getSlot() != null && booking.getSlot().getVehicleType() != null
                    ? booking.getSlot().getVehicleType().getName() : "Phương tiện DriveNow");

            mailer.send(booking.getCustomer().getEmail(),
                    "DriveNow - Xác nhận hoàn tất hoàn tiền cọc đơn #" + booking.getId(), TEMPLATE, vars);

            log.info("Sent refund completed email to {} for booking #{}", booking.getCustomer().getEmail(), booking.getId());
        } catch (Exception e) {
            log.error("Failed to send refund notification email for booking #{}: {}", event.bookingId(), e.getMessage(), e);
        }
    }
}
