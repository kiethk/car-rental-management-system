package com.crms.mvc.notification;

import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Customer;
import com.crms.mvc.entity.Payment;
import com.crms.mvc.event.RefundCompletedEvent;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RefundNotificationListenerTest {
    private final CustomerMailer mailer = mock(CustomerMailer.class);
    private final BookingRepository bookingRepo = mock(BookingRepository.class);
    private final PaymentRepository paymentRepo = mock(PaymentRepository.class);
    private RefundNotificationListener listener;

    @BeforeEach
    void setUp() {
        when(mailer.isConfigured()).thenReturn(true);
        listener = new RefundNotificationListener(mailer, bookingRepo, paymentRepo);
    }

    @Test
    @DisplayName("Gracefully skips email when SMTP username/password not configured")
    void skipsWhenMailNotConfigured() throws Exception {
        when(mailer.isConfigured()).thenReturn(false);

        listener.onRefundCompleted(new RefundCompletedEvent(1L, 2L));

        verifyNoInteractions(bookingRepo);
        verifyNoInteractions(paymentRepo);
        verify(mailer, never()).send(any(), any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("Sends refund email to customer when configured")
    void sendsRefundEmailWhenConfigured() throws Exception {
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

        listener.onRefundCompleted(new RefundCompletedEvent(10L, 20L));

        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> vars = ArgumentCaptor.forClass(Map.class);
        verify(mailer).send(eq("customer@example.com"), subject.capture(), eq("mail/refund-completed"), vars.capture());
        assertThat(subject.getValue()).contains("#10");
        assertThat(vars.getValue()).containsEntry("bankCode", "VCB")
                .containsEntry("accountNumber", "0123456789")
                .containsEntry("accountHolder", "NGUYEN VAN A")
                .containsEntry("refundAmount", new BigDecimal("500000"));
    }

    @Test
    @DisplayName("A counter refund (no bank details) still sends, with empty bank fields")
    void aCounterRefundSendsWithEmptyBankFields() throws Exception {
        Customer customer = new Customer();
        customer.setEmail("customer@example.com");
        Booking booking = new Booking();
        booking.setId(10L);
        booking.setCustomer(customer);
        Payment payment = new Payment();
        payment.setId(20L);
        payment.setAmount(new BigDecimal("500000"));
        payment.setTransactionCode("REFUND-10-1700000000000");
        payment.setPaidAt(OffsetDateTime.now());
        when(bookingRepo.findById(10L)).thenReturn(Optional.of(booking));
        when(paymentRepo.findById(20L)).thenReturn(Optional.of(payment));

        listener.onRefundCompleted(new RefundCompletedEvent(10L, 20L));

        verify(mailer).send(eq("customer@example.com"), any(), eq("mail/refund-completed"), any());
    }

    @Test
    void aMissingBookingOrCustomerEmailSendsNothing() throws Exception {
        when(bookingRepo.findById(10L)).thenReturn(Optional.empty());

        listener.onRefundCompleted(new RefundCompletedEvent(10L, 20L));

        verify(mailer, never()).send(any(), any(), any(), any());
    }
}
