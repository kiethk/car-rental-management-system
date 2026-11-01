package com.crms.mvc.event;

import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Payment;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.PaymentRepository;
import com.crms.mvc.util.DateTimeUtils;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;

/**
 * Listens for completed refund events and notifies the customer via email.
 */
@Component
public class RefundNotificationListener {
    private static final Logger log = LoggerFactory.getLogger(RefundNotificationListener.class);
    private static final String TEMPLATE = "mail/refund-completed";
    private static final Locale VIETNAMESE = Locale.of("vi", "VN");

    private final ObjectProvider<JavaMailSender> mailSender;
    private final ITemplateEngine templateEngine;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final String username;
    private final String password;
    private final String fromAddress;

    public RefundNotificationListener(ObjectProvider<JavaMailSender> mailSender,
                                      ITemplateEngine templateEngine,
                                      BookingRepository bookingRepository,
                                      PaymentRepository paymentRepository,
                                      @Value("${spring.mail.username:}") String username,
                                      @Value("${spring.mail.password:}") String password,
                                      @Value("${crms.mail.from:}") String fromAddress) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.username = username;
        this.password = password;
        this.fromAddress = fromAddress;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRefundCompleted(RefundCompletedEvent event) {
        log.info("Handling RefundCompletedEvent for booking #{} payment #{}", event.bookingId(), event.paymentId());
        try {
            JavaMailSender sender = mailSender.getIfAvailable();
            if (sender == null || username == null || username.isBlank() || password == null || password.isBlank()) {
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

            Context context = new Context(VIETNAMESE);
            context.setVariable("customerName", booking.getCustomer().getName());
            context.setVariable("bookingId", booking.getId());
            context.setVariable("refundAmount", payment.getAmount());
            context.setVariable("bankCode", bankCode);
            context.setVariable("accountNumber", accountNumber);
            context.setVariable("accountHolder", accountHolder);
            context.setVariable("paidAt", DateTimeUtils.toVietnamTime(payment.getPaidAt()));
            context.setVariable("vehicleName", booking.getSlot() != null && booking.getSlot().getVehicleType() != null
                    ? booking.getSlot().getVehicleType().getName() : "Phương tiện DriveNow");

            String body = templateEngine.process(TEMPLATE, context);

            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress == null || fromAddress.isBlank() ? "DriveNow <" + username + ">" : fromAddress);
            helper.setTo(booking.getCustomer().getEmail());
            helper.setSubject("DriveNow - Xác nhận hoàn tất hoàn tiền cọc đơn #" + booking.getId());
            helper.setText(body, true);
            sender.send(message);

            log.info("Sent refund completed email to {} for booking #{}", booking.getCustomer().getEmail(), booking.getId());
        } catch (Exception e) {
            log.error("Failed to send refund notification email for booking #{}: {}", event.bookingId(), e.getMessage(), e);
        }
    }
}

