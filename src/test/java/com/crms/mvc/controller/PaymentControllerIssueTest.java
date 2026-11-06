package com.crms.mvc.controller;

import com.crms.mvc.support.PassThroughSessionAccountService;
import com.crms.mvc.support.TestSessions;
import org.springframework.context.annotation.Import;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Payment;
import com.crms.mvc.service.BookingService;
import com.crms.mvc.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Paying the fees of issues found after settlement, and telling them apart from deposit payments on return. */
@WebMvcTest(PaymentController.class)
@Import(PassThroughSessionAccountService.class)
class PaymentControllerIssueTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private PaymentService paymentService;
    @MockitoBean private BookingService bookingService;

    private MockHttpSession customer;

    @BeforeEach
    void setUp() {
        customer = TestSessions.customer(1L, "John Doe");
        when(paymentService.verifyPaymentSignature(any())).thenReturn(true);
    }

    @Test
    void theCustomerIsSentToTheGatewayForTheOutstandingFees() throws Exception {
        when(paymentService.createIssuePaymentUrl(eq(42L), eq(1L), anyString())).thenReturn("https://sandbox.example/pay?x=1");

        mvc.perform(post("/bookings/42/pay-issue-fees").session(customer))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://sandbox.example/pay?x=1"));
    }

    @Test
    void whenNothingCanBePaidTheCustomerIsToldOnTheBookingPage() throws Exception {
        when(paymentService.createIssuePaymentUrl(eq(42L), eq(1L), anyString()))
                .thenThrow(new IllegalStateException("Không có khoản phí phát sinh nào cần thanh toán"));

        mvc.perform(post("/bookings/42/pay-issue-fees").session(customer))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings/42"))
                .andExpect(flash().attribute("error", "Không có khoản phí phát sinh nào cần thanh toán"));
    }

    @Test
    void aSuccessfulReturnConfirmsTheIssuePaymentAndShowsTheBooking() throws Exception {
        when(paymentService.confirmIssuePayment(77L, "T1", 70000000L)).thenReturn(paymentOfBooking(42L));

        mvc.perform(get("/payment/vnpay/return")
                        .param("vnp_TxnRef", "I77_1700000000000").param("vnp_ResponseCode", "00")
                        .param("vnp_TransactionNo", "T1").param("vnp_Amount", "70000000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings/42"))
                .andExpect(flash().attributeExists("success"));

        verify(paymentService, never()).processDeposit(anyLong(), any(), any());
    }

    @Test
    void aCancelledOrFailedReturnGivesTheFeesBackAsOwed() throws Exception {
        when(paymentService.failIssuePayment(77L)).thenReturn(paymentOfBooking(42L));

        mvc.perform(get("/payment/vnpay/return")
                        .param("vnp_TxnRef", "I77_1700000000000").param("vnp_ResponseCode", "24"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings/42"))
                .andExpect(flash().attributeExists("error"));

        verify(paymentService, never()).confirmIssuePayment(anyLong(), any(), anyLong());
    }

    @Test
    void aReturnWithABadSignatureChangesNothing() throws Exception {
        when(paymentService.verifyPaymentSignature(any())).thenReturn(false);

        mvc.perform(get("/payment/vnpay/return")
                        .param("vnp_TxnRef", "I77_1700000000000").param("vnp_ResponseCode", "00")
                        .param("vnp_Amount", "70000000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));

        verify(paymentService, never()).confirmIssuePayment(anyLong(), any(), anyLong());
        verify(paymentService, never()).failIssuePayment(anyLong());
    }

    @Test
    void aRuleBreakWhileConfirmingComesBackAsAMessage() throws Exception {
        when(paymentService.confirmIssuePayment(77L, "T1", 100L)).thenThrow(new IllegalArgumentException("Payment amount mismatch"));

        mvc.perform(get("/payment/vnpay/return")
                        .param("vnp_TxnRef", "I77_1700000000000").param("vnp_ResponseCode", "00")
                        .param("vnp_TransactionNo", "T1").param("vnp_Amount", "100"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "Payment amount mismatch"));
    }

    @Test
    void depositReturnsStillGoToTheDepositFlow() throws Exception {
        when(paymentService.verifyPaymentSignature(any())).thenReturn(false);

        mvc.perform(get("/payment/vnpay/return").param("vnp_TxnRef", "5_1700000000000").param("vnp_ResponseCode", "00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings/5/pay-deposit"));

        verify(paymentService, never()).confirmIssuePayment(anyLong(), any(), anyLong());
    }

    private Payment paymentOfBooking(Long bookingId) {
        Booking booking = new Booking();
        booking.setId(bookingId);
        Payment payment = new Payment();
        payment.setId(77L);
        payment.setBooking(booking);
        return payment;
    }
}