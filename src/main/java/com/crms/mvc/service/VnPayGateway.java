package com.crms.mvc.service;

import com.crms.mvc.config.VnPayConfig;
import com.crms.mvc.entity.Payment;
import com.crms.mvc.util.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * The VNPay side of a payment and nothing else: it signs the redirect URL the customer is sent to and checks the
 * signature of the answer. What the money means for a booking (deposit, fees, refunds) lives in {@link PaymentService}.
 */
@Component
public class VnPayGateway {
    private static final Logger log = LoggerFactory.getLogger(VnPayGateway.class);

    private final VnPayConfig config;

    public VnPayGateway(VnPayConfig config) {
        this.config = config;
    }

    /** Signs and assembles the VNPay redirect URL; the amount is in dong, VNPay wants it times 100. */
    public String createPaymentUrl(BigDecimal amount, String txnRef, String orderInfo, String clientIp) {
        long amountInCents = amount.multiply(new BigDecimal(100)).longValue();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone(DateTimeUtils.VIETNAM_ZONE));
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone(DateTimeUtils.VIETNAM_ZONE));
        String vnp_CreateDate = formatter.format(cld.getTime());
        cld.add(Calendar.MINUTE, Payment.PAYMENT_TIMEOUT_MINUTES);
        String vnp_ExpireDate = formatter.format(cld.getTime());

        String vnp_IpAddr = (clientIp != null && !clientIp.isBlank()) ? clientIp : "127.0.0.1";

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", config.getVersion());
        vnp_Params.put("vnp_Command", config.getCommand());
        vnp_Params.put("vnp_TmnCode", config.getTmnCode());
        vnp_Params.put("vnp_Amount", String.valueOf(amountInCents));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", txnRef);
        vnp_Params.put("vnp_OrderInfo", orderInfo);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", config.getReturnUrl());
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        try {
            for (String fieldName : fieldNames) {
                String fieldValue = vnp_Params.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    if (hashData.length() > 0) {
                        hashData.append('&');
                        query.append('&');
                    }
                    hashData.append(fieldName).append('=')
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString())).append('=')
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                }
            }

            String vnp_SecureHash = VnPayConfig.hmacSHA512(config.getHashSecret(), hashData.toString());
            String paymentUrl = config.getPayUrl() + "?" + query + "&vnp_SecureHash=" + vnp_SecureHash;
            log.info("Generated VNPay payment URL for reference {}", txnRef);
            return paymentUrl;
        } catch (Exception e) {
            log.error("Failed to generate VNPay payment URL for reference: {}", txnRef, e);
            throw new RuntimeException("Không thể khởi tạo thanh toán VNPay: " + e.getMessage(), e);
        }
    }

    /** True when the query VNPay sent back to the return URL carries a valid signature. */
    public boolean verifySignature(Map<String, String> queryParams) {
        String vnp_SecureHash = queryParams.get("vnp_SecureHash");
        if (vnp_SecureHash == null || vnp_SecureHash.trim().isEmpty()) {
            log.warn("VNPay signature verification failed: missing vnp_SecureHash");
            return false;
        }

        Map<String, String> fields = new HashMap<>();
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            if (key != null && key.startsWith("vnp_") && !key.equals("vnp_SecureHash")
                    && !key.equals("vnp_SecureHashType")) {
                if (val != null && !val.trim().isEmpty()) {
                    fields.put(key, val);
                }
            }
        }

        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        try {
            for (String fieldName : fieldNames) {
                String fieldValue = fields.get(fieldName);
                if (hashData.length() > 0) {
                    hashData.append('&');
                }
                hashData.append(fieldName).append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
            }

            String calculatedHash = VnPayConfig.hmacSHA512(config.getHashSecret(), hashData.toString());
            boolean matches = calculatedHash.equalsIgnoreCase(vnp_SecureHash);
            if (!matches) {
                log.warn("VNPay signature mismatch: invalid signature received");
            }
            return matches;
        } catch (Exception e) {
            log.error("Error during VNPay signature verification", e);
            return false;
        }
    }
}
