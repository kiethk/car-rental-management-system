package com.crms.mvc.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Thanks the customer once a payment after the deposit has been recorded: the rest of the rent at handover, or the
 * fees collected at settlement and for Issues. Runs after the commit and off the request thread, and swallows its own
 * failures, so a mail problem can never undo or block the payment.
 */
@Component
public class PaymentThankYouListener {
    private static final Logger log = LoggerFactory.getLogger(PaymentThankYouListener.class);
    private static final String TEMPLATE = "mail/payment-thank-you";

    private final CustomerMailer mailer;

    public PaymentThankYouListener(CustomerMailer mailer) {
        this.mailer = mailer;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentReceived(PaymentReceivedEvent event) {
        try {
            if (!mailer.isConfigured()) {
                log.info("Email service not configured; skipping thank-you email for booking #{}", event.bookingId());
                return;
            }
            if (event.customerEmail() == null || event.customerEmail().isBlank()) {
                log.warn("Cannot send thank-you email: booking #{} has no customer email", event.bookingId());
                return;
            }
            Map<String, Object> vars = new HashMap<>();
            vars.put("customerName", event.customerName());
            vars.put("bookingId", event.bookingId());
            vars.put("vehicleName", event.vehicleName());
            vars.put("amount", event.amount());
            vars.put("paymentMethod", methodLabel(event.paymentMethod()));
            vars.put("paidAt", event.paidAt());
            vars.put("kind", event.kind().name());
            vars.put("bookingClosed", event.kind() == PaymentReceivedEvent.Kind.SETTLEMENT);
            vars.put("bookingUrl", mailer.url("/bookings/" + event.bookingId()));
            mailer.send(event.customerEmail(), subject(event), TEMPLATE, vars);
        } catch (Exception e) {
            log.error("Failed to send thank-you email for booking #{}: {}", event.bookingId(), e.getMessage(), e);
        }
    }

    private static String subject(PaymentReceivedEvent event) {
        return switch (event.kind()) {
            case RENTAL_BALANCE -> "DriveNow - Cảm ơn bạn đã thanh toán tiền thuê còn lại của đơn #" + event.bookingId();
            case SETTLEMENT -> "DriveNow - Cảm ơn bạn đã quyết toán đơn #" + event.bookingId();
            case ISSUE_FEES -> "DriveNow - Cảm ơn bạn đã thanh toán phí phát sinh của đơn #" + event.bookingId();
        };
    }

    private static String methodLabel(String method) {
        if (method == null) return null;
        return switch (method.toUpperCase()) {
            case "CASH" -> "Tiền mặt";
            case "BANK_TRANSFER" -> "Chuyển khoản";
            case "VNPAY" -> "VNPay";
            default -> method;
        };
    }
}
