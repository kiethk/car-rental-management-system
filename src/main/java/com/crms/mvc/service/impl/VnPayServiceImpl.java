package com.crms.mvc.service.impl;

import com.crms.mvc.config.VnPayConfig;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Payment;
import com.crms.mvc.event.DepositConfirmedEvent;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.PaymentRepository;
import com.crms.mvc.service.BookingFeeService;
import com.crms.mvc.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@Transactional
public class VnPayServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(VnPayServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BookingFeeService bookingFeeService;
    private final ApplicationEventPublisher eventPublisher;
    private final VnPayConfig vnPayConfig;

    public VnPayServiceImpl(PaymentRepository paymentRepository,
                            BookingRepository bookingRepository,
                            BookingFeeService bookingFeeService,
                            ApplicationEventPublisher eventPublisher,
                            VnPayConfig vnPayConfig) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.bookingFeeService = bookingFeeService;
        this.eventPublisher = eventPublisher;
        this.vnPayConfig = vnPayConfig;
    }

    @Override
    @Transactional(readOnly = true)
    public String createDepositPaymentUrl(Booking booking, String clientIp) {
        long amountInCents = booking.getDepositAmount().multiply(new BigDecimal(100)).longValue();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        String vnp_CreateDate = formatter.format(cld.getTime());
        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());

        String vnp_TxnRef = booking.getId() + "_" + System.currentTimeMillis();
        String vnp_IpAddr = (clientIp != null && !clientIp.isBlank()) ? clientIp : "127.0.0.1";

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
            log.info("Generated VNPay deposit payment URL for bookingId {}", booking.getId());
            return paymentUrl;
        } catch (Exception e) {
            log.error("Failed to generate VNPay payment URL for bookingId: {}", booking.getId(), e);
            throw new RuntimeException("Could not initiate VNPay payment: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyPaymentSignature(Map<String, String> queryParams) {
        String vnp_SecureHash = queryParams.get("vnp_SecureHash");
        if (vnp_SecureHash == null || vnp_SecureHash.trim().isEmpty()) {
            log.warn("VNPay signature verification failed: missing vnp_SecureHash");
            return false;
        }

        Map<String, String> fields = new HashMap<>();
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
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
                log.warn("VNPay signature mismatch: invalid signature received");
            }
            return matches;
        } catch (Exception e) {
            log.error("Error during VNPay signature verification", e);
            return false;
        }
    }

    @Override
    public Payment processDeposit(Long bookingId) {
        return processDeposit(bookingId, null, "VNPAY");
    }

    @Override
    public Payment processDeposit(Long bookingId, String transactionNo, String paymentMethod) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Booking not found: " + bookingId));

        if ("confirmed".equalsIgnoreCase(booking.getStatus())) {
            return paymentRepository.findByBookingIdAndPaymentType(bookingId, "DEPOSIT").orElse(null);
        }

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPaymentType("DEPOSIT");
        payment.setTransactionCode(transactionNo != null ? transactionNo : "TXN-DEP-" + bookingId + "-" + System.currentTimeMillis());
        payment.setPaymentMethod(paymentMethod != null ? paymentMethod : "VNPAY");
        payment.setAmount(booking.getDepositAmount());
        payment.setStatus("paid");
        payment.setPaidAt(OffsetDateTime.now());
        paymentRepository.save(payment);

        booking.setStatus("confirmed");
        bookingRepository.save(booking);

        eventPublisher.publishEvent(new DepositConfirmedEvent(bookingId));

        return payment;
    }

    @Override
    public Payment processSettlement(Long bookingId, BigDecimal lateFee, BigDecimal additionalFine) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Booking not found: " + bookingId));

        BigDecimal initialDeposit = booking.getDepositAmount() != null ? booking.getDepositAmount() : BigDecimal.ZERO;
        BigDecimal rentAmount = booking.getTotalPrice() != null ? booking.getTotalPrice() : BigDecimal.ZERO;

        BigDecimal safeLateFee = lateFee != null ? lateFee : BigDecimal.ZERO;
        BigDecimal safeFine = additionalFine != null ? additionalFine : BigDecimal.ZERO;

        if (safeLateFee.compareTo(BigDecimal.ZERO) > 0) {
            bookingFeeService.recordFee(bookingId, "LATE_FEE", safeLateFee, "Late return fee", null);
        }
        if (safeFine.compareTo(BigDecimal.ZERO) > 0) {
            bookingFeeService.recordFee(bookingId, "DAMAGE_COMPENSATION", safeFine, "Fine for issues during trip", null);
        }

        booking.setLateFee(safeLateFee);

        BigDecimal totalPayable = rentAmount.add(safeLateFee).add(safeFine);
        BigDecimal remaining = totalPayable.subtract(initialDeposit);

        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            BigDecimal refundAmount = remaining.abs();
            Payment refundPayment = new Payment();
            refundPayment.setBooking(booking);
            refundPayment.setPaymentType("REFUND");
            refundPayment.setAmount(refundAmount);
            refundPayment.setStatus("paid");
            refundPayment.setPaymentMethod("VNPAY");
            refundPayment.setTransactionCode("REFUND-" + bookingId + "-" + System.currentTimeMillis());
            refundPayment.setPaidAt(OffsetDateTime.now());
            paymentRepository.save(refundPayment);

            booking.setTotalPrice(totalPayable);
            bookingRepository.save(booking);
            return refundPayment;
        }

        Payment finalPayment = new Payment();
        finalPayment.setBooking(booking);
        finalPayment.setPaymentType("FINAL");
        finalPayment.setAmount(remaining);
        finalPayment.setStatus("paid");
        finalPayment.setPaymentMethod("VNPAY");
        finalPayment.setTransactionCode("FINAL-" + bookingId + "-" + System.currentTimeMillis());
        finalPayment.setPaidAt(OffsetDateTime.now());
        paymentRepository.save(finalPayment);

        booking.setTotalPrice(totalPayable);
        bookingRepository.save(booking);

        return finalPayment;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByBooking(Long bookingId) {
        return paymentRepository.findByBookingIdOrderByPaidAtAsc(bookingId);
    }
}
