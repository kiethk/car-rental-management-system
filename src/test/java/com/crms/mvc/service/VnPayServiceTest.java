package com.crms.mvc.service;

import com.crms.mvc.config.VnPayConfig;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Payment;
import com.crms.mvc.event.RefundCompletedEvent;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.PaymentRepository;
import com.crms.mvc.service.impl.VnPayServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class VnPayServiceTest {

    private PaymentService paymentService;
    private PaymentRepository paymentRepository;
    private BookingRepository bookingRepository;
    private ApplicationEventPublisher eventPublisher;
    private VnPayConfig vnPayConfig;

    @BeforeEach
    public void setUp() {
        vnPayConfig = new VnPayConfig();
        ReflectionTestUtils.setField(vnPayConfig, "tmnCode", "CGXZLS0Z");
        ReflectionTestUtils.setField(vnPayConfig, "hashSecret", "XNBCJFAKAZQSGZSSTGIRQUXNMZFVOQD8");
        ReflectionTestUtils.setField(vnPayConfig, "payUrl", "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        ReflectionTestUtils.setField(vnPayConfig, "returnUrl", "http://localhost:8081/payment/vnpay/return");
        ReflectionTestUtils.setField(vnPayConfig, "version", "2.1.0");
        ReflectionTestUtils.setField(vnPayConfig, "command", "pay");

        paymentRepository = Mockito.mock(PaymentRepository.class);
        bookingRepository = Mockito.mock(BookingRepository.class);
        BookingFeeService bookingFeeService = Mockito.mock(BookingFeeService.class);
        eventPublisher = Mockito.mock(ApplicationEventPublisher.class);

        paymentService = new VnPayServiceImpl(paymentRepository, bookingRepository, bookingFeeService, eventPublisher, vnPayConfig);
    }

    @Test
    public void testCreateDepositPaymentUrl() {
        Booking booking = new Booking();
        booking.setId(7L);
        booking.setDepositAmount(new BigDecimal("675000"));

        String url = paymentService.createDepositPaymentUrl(booking, "127.0.0.1");

        assertNotNull(url);
        assertTrue(url.startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?"));
        assertTrue(url.contains("vnp_Amount=67500000"));
        assertTrue(url.contains("vnp_TmnCode=CGXZLS0Z"));
        assertTrue(url.contains("vnp_SecureHash="));
    }

    @Test
    public void testVerifyPaymentSignature_Success() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "67500000");
        params.put("vnp_BankCode", "NCB");
        params.put("vnp_Command", "pay");
        params.put("vnp_OrderInfo", "DriveNow_Deposit_Booking_7");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TmnCode", "CGXZLS0Z");
        params.put("vnp_TxnRef", "7_123456789");

        // Calculate expected hash
        String hashData = "vnp_Amount=67500000&vnp_BankCode=NCB&vnp_Command=pay&vnp_OrderInfo=DriveNow_Deposit_Booking_7&vnp_ResponseCode=00&vnp_TmnCode=CGXZLS0Z&vnp_TxnRef=7_123456789";
        String expectedHash = VnPayConfig.hmacSHA512("XNBCJFAKAZQSGZSSTGIRQUXNMZFVOQD8", hashData);
        params.put("vnp_SecureHash", expectedHash);

        boolean isValid = paymentService.verifyPaymentSignature(params);
        assertTrue(isValid);
    }

    @Test
    public void testVerifyPaymentSignature_Failure() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "67500000");
        params.put("vnp_SecureHash", "INVALID_HASH");

        boolean isValid = paymentService.verifyPaymentSignature(params);
        assertFalse(isValid);
    }

    @Test
    public void testCreatePendingRefund() {
        Booking booking = new Booking();
        booking.setId(10L);
        booking.setDepositAmount(new BigDecimal("500000"));

        Mockito.when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        Mockito.when(paymentRepository.save(Mockito.any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment refund = paymentService.createPendingRefund(10L, new BigDecimal("500000"), "VCB", "1234567890", "NGUYEN VAN A");

        assertNotNull(refund);
        assertEquals("REFUND", refund.getPaymentType());
        assertEquals("pending", refund.getStatus());
        assertEquals(new BigDecimal("500000"), refund.getAmount());
        assertEquals("VCB: 1234567890 - NGUYEN VAN A", refund.getTransactionCode());
    }

    @Test
    public void testConfirmRefundCompleted() {
        Booking booking = new Booking();
        booking.setId(10L);

        Payment payment = new Payment();
        payment.setId(100L);
        payment.setBooking(booking);
        payment.setPaymentType("REFUND");
        payment.setStatus("pending");
        payment.setTransactionCode("VCB: 1234567890 - NGUYEN VAN A");

        Mockito.when(paymentRepository.findById(100L)).thenReturn(Optional.of(payment));
        Mockito.when(paymentRepository.save(Mockito.any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment confirmed = paymentService.confirmRefundCompleted(100L, 1L, null);

        assertNotNull(confirmed);
        assertEquals("paid", confirmed.getStatus());
        assertNotNull(confirmed.getPaidAt());
        Mockito.verify(eventPublisher, Mockito.times(1)).publishEvent(Mockito.any(RefundCompletedEvent.class));
    }
}
