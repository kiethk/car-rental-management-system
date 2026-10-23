package com.crms.mvc.service;

import com.crms.mvc.entity.Booking;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public interface VnPayService {
    String createDepositPaymentUrl(Booking booking, HttpServletRequest request);
    boolean verifyPaymentSignature(Map<String, String> vnpParams);
}

