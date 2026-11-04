package com.crms.mvc.event;

import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Customer;
import com.crms.mvc.entity.Payment;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.PaymentRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RefundNotificationListenerTest {

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("Gracefully skips email when SMTP username/password not configured")
    void skipsWhenMailNotConfigured() {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        ITemplateEngine templateEngine = mock(ITemplateEngine.class);
        BookingRepository bookingRepo = mock(BookingRepository.class);
        PaymentRepository paymentRepo = mock(PaymentRepository.class);

        RefundNotificationListener listener = new RefundNotificationListener(
                provider, templateEngine, bookingRepo, paymentRepo, "", "", ""
        );

        listener.onRefundCompleted(new RefundCompletedEvent(1L, 2L));

        verifyNoInteractions(bookingRepo);
        verifyNoInteractions(paymentRepo);
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("Sends refund email to customer when configured")
    void sendsRefundEmailWhenConfigured() {
        JavaMailSender sender = mock(JavaMailSender.class);
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);

        ITemplateEngine templateEngine = mock(ITemplateEngine.class);
        when(templateEngine.process(eq("mail/refund-completed"), any(Context.class))).thenReturn("<html>Refund</html>");

        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(mimeMessage);

        BookingRepository bookingRepo = mock(BookingRepository.class);
        PaymentRepository paymentRepo = mock(PaymentRepository.class);

        Customer customer = new Customer();
        customer.setName("Nguyen Van A");
        customer.setEmail("customer@example.com");

        Booking booking = new Booking();
        booking.setId(10L);
        booking.setCustomer(customer);

        Payment payment = new Payment();
        payment.setId(20L);
        payment.setAmount(new BigDecimal("500000"));
        payment.setTransactionCode("VCB: 0123456789 - NGUYEN VAN A");
        payment.setPaidAt(OffsetDateTime.now());

        when(bookingRepo.findById(10L)).thenReturn(Optional.of(booking));
        when(paymentRepo.findById(20L)).thenReturn(Optional.of(payment));

        RefundNotificationListener listener = new RefundNotificationListener(
                provider, templateEngine, bookingRepo, paymentRepo, "admin@drivenow.com", "secretpass", "DriveNow <admin@drivenow.com>"
        );

        listener.onRefundCompleted(new RefundCompletedEvent(10L, 20L));

        verify(sender, times(1)).send(mimeMessage);
    }
}

