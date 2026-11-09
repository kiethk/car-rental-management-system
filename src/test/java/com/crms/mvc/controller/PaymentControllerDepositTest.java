package com.crms.mvc.controller;

import com.crms.mvc.entity.Booking;
import com.crms.mvc.dto.DepositSessionDTO;
import com.crms.mvc.service.BookingService;
import com.crms.mvc.service.PaymentService;
import com.crms.mvc.service.VnPayGateway;
import com.crms.mvc.support.PassThroughSessionAccountService;
import com.crms.mvc.support.TestSessions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

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

/** Characterization of the deposit payment flow in {@link PaymentController}: starting it, and the gateway return. */
@WebMvcTest(PaymentController.class)
@Import(PassThroughSessionAccountService.class)
class PaymentControllerDepositTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private PaymentService paymentService;
    @MockitoBean private VnPayGateway gateway;
    @MockitoBean private BookingService bookingService;

    private MockHttpSession customer;

    @BeforeEach
    void setUp() {
        customer = TestSessions.customer(1L, "John Doe");
        when(gateway.verifySignature(any())).thenReturn(true);
    }

    // ---- starting the payment ----

    @Test
    void anUnknownBookingGoesBackToMyBookings() throws Exception {
        mvc.perform(post("/bookings/42/pay-deposit").session(customer))
                .andExpect(redirectedUrl("/bookings/my-bookings"))
                .andExpect(flash().attribute("error", "Không tìm thấy đơn đặt xe."));
    }

    @Test
    void someoneElsesBookingCannotBePaid() throws Exception {
        givenBooking(42L, 2L, Booking.STATUS_PENDING_PAYMENT, OffsetDateTime.now());

        mvc.perform(post("/bookings/42/pay-deposit").session(customer))
                .andExpect(redirectedUrl("/bookings/my-bookings"))
                .andExpect(flash().attribute("error", "Bạn không có quyền thanh toán đơn đặt xe này."));
    }

    @Test
    void anAlreadyPaidBookingGoesStraightToTheSuccessPage() throws Exception {
        givenBooking(42L, 1L, Booking.STATUS_CONFIRMED, OffsetDateTime.now());

        mvc.perform(post("/bookings/42/pay-deposit").session(customer))
                .andExpect(redirectedUrl("/bookings/42/success"));
    }

    @Test
    void aBookingThatIsNotWaitingForTheDepositIsExplained() throws Exception {
        givenBooking(42L, 1L, Booking.STATUS_CANCELLED, OffsetDateTime.now());

        mvc.perform(post("/bookings/42/pay-deposit").session(customer))
                .andExpect(redirectedUrl("/bookings/42"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void aBookingLeftUnpaidForMoreThanFifteenMinutesIsCancelled() throws Exception {
        givenBooking(42L, 1L, Booking.STATUS_PENDING_PAYMENT, OffsetDateTime.now().minusMinutes(16));

        mvc.perform(post("/bookings/42/pay-deposit").session(customer))
                .andExpect(redirectedUrl("/bookings/42"))
                .andExpect(flash().attribute("error", "Đơn đặt xe đã hết hạn thanh toán cọc sau 15 phút và đã tự động hủy."));

        verify(bookingService).cancelUnpaidBooking(42L, 1L);
        verify(paymentService, never()).createDepositPaymentUrl(anyLong(), anyString());
    }

    @Test
    void aFreshPendingBookingIsSentToTheGateway() throws Exception {
        givenBooking(42L, 1L, Booking.STATUS_PENDING_PAYMENT, OffsetDateTime.now());
        when(paymentService.createDepositPaymentUrl(eq(42L), anyString())).thenReturn("https://sandbox.example/pay?x=1");

        mvc.perform(post("/bookings/42/pay-deposit").session(customer))
                .andExpect(redirectedUrl("https://sandbox.example/pay?x=1"));
    }

    @Test
    void aGatewayFailureComesBackAsAMessageOnTheDepositPage() throws Exception {
        givenBooking(42L, 1L, Booking.STATUS_PENDING_PAYMENT, OffsetDateTime.now());
        when(paymentService.createDepositPaymentUrl(eq(42L), anyString())).thenThrow(new IllegalStateException("down"));

        mvc.perform(post("/bookings/42/pay-deposit").session(customer))
                .andExpect(redirectedUrl("/bookings/42/pay-deposit"))
                .andExpect(flash().attributeExists("error"));
    }

    // ---- the gateway return ----

    @Test
    void aSuccessfulReturnConfirmsTheDeposit() throws Exception {
        givenBooking(5L, 1L, Booking.STATUS_PENDING_PAYMENT, OffsetDateTime.now());

        mvc.perform(get("/payment/vnpay/return").param("vnp_TxnRef", "5_1700000000000").param("vnp_ResponseCode", "00")
                        .param("vnp_TransactionNo", "T9").param("vnp_Amount", "30000000"))
                .andExpect(redirectedUrl("/bookings/5/success?vnp_TxnRef=5_1700000000000&vnp_TransactionNo=T9"));

        verify(paymentService).confirmDeposit(5L, "T9", "VNPAY");
    }

    @Test
    void aReturnForABookingInTheWrongStateIsRefused() throws Exception {
        givenBooking(5L, 1L, Booking.STATUS_CANCELLED, OffsetDateTime.now());

        mvc.perform(get("/payment/vnpay/return").param("vnp_TxnRef", "5_1700000000000").param("vnp_ResponseCode", "00")
                        .param("vnp_Amount", "30000000"))
                .andExpect(redirectedUrl("/bookings/5"))
                .andExpect(flash().attributeExists("error"));

        verify(paymentService, never()).confirmDeposit(anyLong(), any(), any());
    }

    @Test
    void aReturnWithAnotherAmountThanTheDepositIsRefused() throws Exception {
        givenBooking(5L, 1L, Booking.STATUS_PENDING_PAYMENT, OffsetDateTime.now());

        mvc.perform(get("/payment/vnpay/return").param("vnp_TxnRef", "5_1700000000000").param("vnp_ResponseCode", "00")
                        .param("vnp_Amount", "1"))
                .andExpect(redirectedUrl("/bookings/5/pay-deposit"))
                .andExpect(flash().attribute("error", "Số tiền thanh toán không khớp."));

        verify(paymentService, never()).confirmDeposit(anyLong(), any(), any());
    }

    @Test
    void aCancelledPaymentAtTheGatewayCanBeRetried() throws Exception {
        mvc.perform(get("/payment/vnpay/return").param("vnp_TxnRef", "5_1700000000000").param("vnp_ResponseCode", "24"))
                .andExpect(redirectedUrl("/bookings/5/pay-deposit"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void aReturnForAnUnknownBookingGoesHome() throws Exception {
        mvc.perform(get("/payment/vnpay/return").param("vnp_TxnRef", "5_1700000000000").param("vnp_ResponseCode", "00"))
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("error", "Không tìm thấy đơn đặt xe: 5"));
    }

    private void givenBooking(Long id, Long ownerId, String status, OffsetDateTime createdAt) {
        when(paymentService.findDepositSession(id)).thenReturn(Optional.of(
                new DepositSessionDTO(id, ownerId, status, new BigDecimal("300000"), createdAt)));
    }
}
