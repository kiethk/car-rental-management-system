package com.crms.mvc.service;

import com.crms.mvc.config.VnPayConfig;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.service.impl.VnPayServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class VnPayServiceTest {

    private VnPayService vnPayService;
    private VnPayConfig vnPayConfig;

    @BeforeEach
    public void setUp() {
        vnPayConfig = new VnPayConfig();
        ReflectionTestUtils.setField(vnPayConfig, "tmnCode", "CGXZLS0Z");
        ReflectionTestUtils.setField(vnPayConfig, "hashSecret", "XNBCJFAKAZQSGZSSTGIRQUXNMZFVOQD8");
        ReflectionTestUtils.setField(vnPayConfig, "payUrl", "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        ReflectionTestUtils.setField(vnPayConfig, "returnUrl", "http://localhost:8081/payment/vnpay-return");
        ReflectionTestUtils.setField(vnPayConfig, "version", "2.1.0");
        ReflectionTestUtils.setField(vnPayConfig, "command", "pay");

        vnPayService = new VnPayServiceImpl(vnPayConfig);
    }

    @Test
    public void testCreateDepositPaymentUrl() {
        Booking booking = new Booking();
        booking.setId(7L);
        booking.setDepositAmount(new BigDecimal("675000"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        String url = vnPayService.createDepositPaymentUrl(booking, request);

        assertNotNull(url);
        assertTrue(url.startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?"));
        assertTrue(url.contains("vnp_TmnCode=CGXZLS0Z"));
        assertTrue(url.contains("vnp_Amount=67500000"));
        assertTrue(url.contains("vnp_Command=pay"));
        assertTrue(url.contains("vnp_Version=2.1.0"));
        assertTrue(url.contains("vnp_SecureHash="));
    }

    @Test
    public void testVerifyPaymentSignature_Success() {
        Booking booking = new Booking();
        booking.setId(9L);
        booking.setDepositAmount(new BigDecimal("500000"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        String url = vnPayService.createDepositPaymentUrl(booking, request);

        // Parse generated params from URL
        String query = url.substring(url.indexOf("?") + 1);
        Map<String, String> params = new HashMap<>();
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=");
            String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String val = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            params.put(key, val);
        }

        // Verify that the signature generated is validated by verifyPaymentSignature
        boolean valid = vnPayService.verifyPaymentSignature(params);
        assertTrue(valid, "Signature verification should succeed for authentic params");

        // Now test tampered param (e.g. amount modified)
        params.put("vnp_Amount", "99999999");
        boolean tampered = vnPayService.verifyPaymentSignature(params);
        assertFalse(tampered, "Signature verification should fail when amount is modified");
    }
}

