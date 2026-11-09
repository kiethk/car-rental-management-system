package com.crms.mvc.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.crms.mvc.event.PaymentReceivedEvent;

/** The customer is thanked after paying; a mail problem must never break the payment. */
class PaymentThankYouListenerTest {
    private CustomerMailer mailer;
    private PaymentThankYouListener listener;

    @BeforeEach
    void setUp() {
        mailer = mock(CustomerMailer.class);
        when(mailer.isConfigured()).thenReturn(true);
        when(mailer.url(any())).thenReturn("http://localhost:8081/bookings/42");
        listener = new PaymentThankYouListener(mailer);
    }

    @Test
    @SuppressWarnings("unchecked")
    void theRemainingRentIsThankedWithTheAmountAndTheMethod() throws Exception {
        listener.onPaymentReceived(event(PaymentReceivedEvent.Kind.RENTAL_BALANCE, "CASH", "an@example.com"));

        ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        verify(mailer).send(eq("an@example.com"), subject.capture(), eq("mail/payment-thank-you"), variables.capture());
        assertThat(subject.getValue()).contains("tiền thuê còn lại").contains("#42");
        assertThat(variables.getValue()).containsEntry("kind", "RENTAL_BALANCE")
                .containsEntry("paymentMethod", "Tiền mặt")
                .containsEntry("bookingClosed", false)
                .containsEntry("amount", new BigDecimal("500000"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void aSettlementThankYouSaysTheBookingIsClosed() throws Exception {
        listener.onPaymentReceived(event(PaymentReceivedEvent.Kind.SETTLEMENT, "BANK_TRANSFER", "an@example.com"));

        ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        verify(mailer).send(eq("an@example.com"), subject.capture(), eq("mail/payment-thank-you"), variables.capture());
        assertThat(subject.getValue()).contains("quyết toán");
        assertThat(variables.getValue()).containsEntry("bookingClosed", true).containsEntry("paymentMethod", "Chuyển khoản");
    }

    @Test
    void stayQuietWhenMailIsNotConfiguredOrThereIsNoAddress() throws Exception {
        when(mailer.isConfigured()).thenReturn(false);
        listener.onPaymentReceived(event(PaymentReceivedEvent.Kind.ISSUE_FEES, "VNPAY", "an@example.com"));

        when(mailer.isConfigured()).thenReturn(true);
        listener.onPaymentReceived(event(PaymentReceivedEvent.Kind.ISSUE_FEES, "VNPAY", null));

        verify(mailer, never()).send(any(), any(), any(), any());
    }

    @Test
    void aMailFailureIsSwallowedSoThePaymentIsNeverBroken() throws Exception {
        org.mockito.Mockito.doThrow(new IllegalStateException("smtp down")).when(mailer).send(any(), any(), any(), any());

        listener.onPaymentReceived(event(PaymentReceivedEvent.Kind.ISSUE_FEES, "VNPAY", "an@example.com"));
    }

    private static PaymentReceivedEvent event(PaymentReceivedEvent.Kind kind, String method, String email) {
        return new PaymentReceivedEvent(42L, "Nguyễn Văn An", email, "Toyota Vios", new BigDecimal("500000"),
                method, OffsetDateTime.parse("2026-11-08T14:30:00+07:00"), kind);
    }
}
