package com.crms.mvc.service.impl;

import com.crms.mvc.config.VnPayConfig;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.service.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class VnPayServiceImpl implements VnPayService {

    private static final Logger log = LoggerFactory.getLogger(VnPayServiceImpl.class);

    private final VnPayConfig vnPayConfig;

    public VnPayServiceImpl(VnPayConfig vnPayConfig) {
        this.vnPayConfig = vnPayConfig;
    }

    @Override
    public String createDepositPaymentUrl(Booking booking, HttpServletRequest request) {
        long amountInCents = booking.getDepositAmount().multiply(new BigDecimal(100)).longValue();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        String vnp_CreateDate = formatter.format(cld.getTime());
        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());

        String vnp_TxnRef = booking.getId() + "_" + System.currentTimeMillis();
        String vnp_IpAddr = VnPayConfig.getIpAddress(request);

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnPayConfig.getVersion());
        vnp_Params.put("vnp_Command", vnPayConfig.getCommand());
        vnp_Params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnp_Params.put("vnp_Amount", String.valueOf(amountInCents));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "DriveNow_Deposit_Booking_" + booking.getId());
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
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
                    hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString())).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                }
            }

            String vnp_SecureHash = VnPayConfig.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
            String paymentUrl = vnPayConfig.getPayUrl() + "?" + query.toString() + "&vnp_SecureHash=" + vnp_SecureHash;
            log.info("Generated VNPay deposit payment URL for bookingId {}: {}", booking.getId(), paymentUrl);
            return paymentUrl;
        } catch (Exception e) {
            log.error("Failed to generate VNPay payment URL for bookingId: {}", booking.getId(), e);
            throw new RuntimeException("Could not initiate VNPay payment: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verifyPaymentSignature(Map<String, String> vnpParams) {
        String vnp_SecureHash = vnpParams.get("vnp_SecureHash");
        if (vnp_SecureHash == null || vnp_SecureHash.trim().isEmpty()) {
            log.warn("VNPay signature verification failed: missing vnp_SecureHash");
            return false;
        }

        Map<String, String> fields = new HashMap<>();
        for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            if (key != null && key.startsWith("vnp_") && !key.equals("vnp_SecureHash") && !key.equals("vnp_SecureHashType")) {
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
                hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
            }

            String calculatedHash = VnPayConfig.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
            boolean matches = calculatedHash.equalsIgnoreCase(vnp_SecureHash);
            if (!matches) {
                log.warn("VNPay signature mismatch: received={}, calculated={}, rawData={}", vnp_SecureHash, calculatedHash, hashData);
            }
            return matches;
        } catch (Exception e) {
            log.error("Error during VNPay signature verification", e);
            return false;
        }
    }
}

