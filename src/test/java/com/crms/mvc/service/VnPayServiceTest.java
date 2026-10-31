package com.crms.mvc.service;

import com.crms.mvc.config.VnPayConfig;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.PaymentRepository;
import com.crms.mvc.service.impl.VnPayServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class VnPayServiceTest {

    private PaymentService paymentService;
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

        PaymentRepository paymentRepository = Mockito.mock(PaymentRepository.class);
        BookingRepository bookingRepository = Mockito.mock(BookingRepository.class);
        BookingFeeService bookingFeeService = Mockito.mock(BookingFeeService.class);
        ApplicationEventPublisher eventPublisher = Mockito.mock(ApplicationEventPublisher.class);

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
}
