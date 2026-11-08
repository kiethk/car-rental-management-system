package com.crms.mvc.event;

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
 * Emails the voucher bù to a customer whose booking was cancelled for lack of a car. Runs after the commit and off
 * the request thread, and swallows its own failures: the voucher still shows on the booking page.
 */
@Component
public class CompensationVoucherMailListener {
    private static final Logger log = LoggerFactory.getLogger(CompensationVoucherMailListener.class);
    private static final Locale VIETNAMESE = Locale.of("vi", "VN");

    private final ObjectProvider<JavaMailSender> mailSender;
    private final ITemplateEngine templateEngine;
    private final String username;
    private final String password;
    private final String fromAddress;
    private final String baseUrl;

    public CompensationVoucherMailListener(ObjectProvider<JavaMailSender> mailSender,
                                           ITemplateEngine templateEngine,
                                           @Value("${spring.mail.username:}") String username,
                                           @Value("${spring.mail.password:}") String password,
                                           @Value("${crms.mail.from:}") String fromAddress,
                                           @Value("${crms.base-url:http://localhost:8081}") String baseUrl) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.username = username;
        this.password = password;
        this.fromAddress = fromAddress;
        this.baseUrl = baseUrl;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVoucherIssued(CompensationVoucherIssuedEvent event) {
        try {
            JavaMailSender sender = mailSender.getIfAvailable();
            if (sender == null || username == null || username.isBlank() || password == null || password.isBlank()) {
                log.info("Email service not configured; skipping voucher email for booking #{}", event.bookingId());
                return;
            }
            if (event.customerEmail() == null) {
                log.warn("Cannot send voucher email: no customer email for booking #{}", event.bookingId());
                return;
            }
            Context context = new Context(VIETNAMESE);
            context.setVariable("customerName", event.customerName());
            context.setVariable("bookingId", event.bookingId());
            context.setVariable("code", event.code());
            context.setVariable("discountPercent", event.discountPercent());
            context.setVariable("validUntil", event.validUntil());
            context.setVariable("searchUrl", baseUrl + "/search");
            String body = templateEngine.process("mail/compensation-voucher", context);

            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress == null || fromAddress.isBlank() ? "DriveNow <" + username + ">" : fromAddress);
            helper.setTo(event.customerEmail());
            helper.setSubject("DriveNow - Xin lỗi vì đơn #" + event.bookingId() + ": tặng bạn voucher giảm "
                    + event.discountPercent() + "%");
            helper.setText(body, true);
            sender.send(message);
            log.info("Sent compensation voucher for booking #{}", event.bookingId());
        } catch (Exception e) {
            log.error("Failed to send voucher email for booking #{}: {}", event.bookingId(), e.getMessage(), e);
        }
    }
}
