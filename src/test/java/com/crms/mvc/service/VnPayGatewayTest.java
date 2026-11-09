package com.crms.mvc.service;

import com.crms.mvc.config.VnPayConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** The VNPay gateway signs what it sends and trusts only what it can verify. */
class VnPayGatewayTest {
    private static final String SECRET = "XNBCJFAKAZQSGZSSTGIRQUXNMZFVOQD8";
    private VnPayGateway gateway;

    @BeforeEach
    void setUp() {
        VnPayConfig config = new VnPayConfig();
        ReflectionTestUtils.setField(config, "tmnCode", "CGXZLS0Z");
        ReflectionTestUtils.setField(config, "hashSecret", SECRET);
        ReflectionTestUtils.setField(config, "payUrl", "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        ReflectionTestUtils.setField(config, "returnUrl", "http://localhost:8081/payment/vnpay/return");
        ReflectionTestUtils.setField(config, "version", "2.1.0");
        ReflectionTestUtils.setField(config, "command", "pay");
        gateway = new VnPayGateway(config);
    }

    @Test
    void theRedirectUrlCarriesTheAmountInCentsTheMerchantAndASignature() {
        String url = gateway.createPaymentUrl(new BigDecimal("675000"), "7_1700000000000", "DriveNow_Deposit_Booking_7", "10.0.0.5");

        assertThat(url).startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?")
                .contains("vnp_Amount=67500000")
                .contains("vnp_TmnCode=CGXZLS0Z")
                .contains("vnp_TxnRef=7_1700000000000")
                .contains("vnp_IpAddr=10.0.0.5")
                .contains("vnp_SecureHash=");
    }

    @Test
    void aMissingClientIpFallsBackToLocalhost() {
        assertThat(gateway.createPaymentUrl(new BigDecimal("1000"), "1_1", "info", null)).contains("vnp_IpAddr=127.0.0.1");
        assertThat(gateway.createPaymentUrl(new BigDecimal("1000"), "1_1", "info", "  ")).contains("vnp_IpAddr=127.0.0.1");
    }

    @Test
    void aCorrectlySignedReturnIsAccepted() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "67500000");
        params.put("vnp_BankCode", "NCB");
        params.put("vnp_Command", "pay");
        params.put("vnp_OrderInfo", "DriveNow_Deposit_Booking_7");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TmnCode", "CGXZLS0Z");
        params.put("vnp_TxnRef", "7_123456789");
        String hashData = "vnp_Amount=67500000&vnp_BankCode=NCB&vnp_Command=pay&vnp_OrderInfo=DriveNow_Deposit_Booking_7"
                + "&vnp_ResponseCode=00&vnp_TmnCode=CGXZLS0Z&vnp_TxnRef=7_123456789";
        params.put("vnp_SecureHash", VnPayConfig.hmacSHA512(SECRET, hashData));

        assertThat(gateway.verifySignature(params)).isTrue();
    }

    @Test
    void aTamperedOrUnsignedReturnIsRefused() {
        Map<String, String> tampered = new HashMap<>();
        tampered.put("vnp_Amount", "67500000");
        tampered.put("vnp_SecureHash", "INVALID_HASH");
        Map<String, String> unsigned = new HashMap<>();
        unsigned.put("vnp_Amount", "67500000");

        assertThat(gateway.verifySignature(tampered)).isFalse();
        assertThat(gateway.verifySignature(unsigned)).isFalse();
    }

    @Test
    void whatWeSignWeAlsoVerify() {
        String url = gateway.createPaymentUrl(new BigDecimal("500000"), "9_1", "info", "127.0.0.1");
        Map<String, String> params = new HashMap<>();
        for (String pair : url.substring(url.indexOf('?') + 1).split("&")) {
            String[] kv = pair.split("=", 2);
            params.put(kv[0], java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.US_ASCII));
        }

        assertThat(gateway.verifySignature(params)).isTrue();
        params.put("vnp_Amount", "1");
        assertThat(gateway.verifySignature(params)).isFalse();
    }
}
