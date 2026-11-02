package com.crms.mvc.service.impl;

import com.crms.mvc.config.VnPayConfig;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.BookingFee;
import com.crms.mvc.entity.Payment;
import com.crms.mvc.event.DepositConfirmedEvent;
import com.crms.mvc.event.RefundCompletedEvent;
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
        return buildPaymentUrl(booking.getDepositAmount(), booking.getId() + "_" + System.currentTimeMillis(),
                "DriveNow_Deposit_Booking_" + booking.getId(), clientIp);
    }

    /** Signs and assembles the VNPay redirect URL; the amount is in dong, VNPay wants it times 100. */
    private String buildPaymentUrl(BigDecimal amount, String txnRef, String orderInfo, String clientIp) {
        long amountInCents = amount.multiply(new BigDecimal(100)).longValue();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        String vnp_CreateDate = formatter.format(cld.getTime());
        cld.add(Calendar.MINUTE, Payment.PAYMENT_TIMEOUT_MINUTES);
        String vnp_ExpireDate = formatter.format(cld.getTime());

        String vnp_IpAddr = (clientIp != null && !clientIp.isBlank()) ? clientIp : "127.0.0.1";

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnPayConfig.getVersion());
        vnp_Params.put("vnp_Command", vnPayConfig.getCommand());
        vnp_Params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnp_Params.put("vnp_Amount", String.valueOf(amountInCents));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", txnRef);
        vnp_Params.put("vnp_OrderInfo", orderInfo);
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
                    hashData.append(fieldName).append('=')
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString())).append('=')
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                }
            }

            String vnp_SecureHash = VnPayConfig.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
            String paymentUrl = vnPayConfig.getPayUrl() + "?" + query.toString() + "&vnp_SecureHash=" + vnp_SecureHash;
            log.info("Generated VNPay payment URL for reference {}", txnRef);
            return paymentUrl;
        } catch (Exception e) {
            log.error("Failed to generate VNPay payment URL for reference: {}", txnRef, e);
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
        return processDeposit(bookingId, null, Payment.METHOD_VNPAY);
    }

    @Override
    public Payment processDeposit(Long bookingId, String transactionNo, String paymentMethod) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn thuê: " + bookingId));

        if ("confirmed".equalsIgnoreCase(booking.getStatus())) {
            return paymentRepository.findByBookingIdAndPaymentType(bookingId, Payment.TYPE_DEPOSIT).orElse(null);
        }

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPaymentType(Payment.TYPE_DEPOSIT);
        payment.setTransactionCode(
                transactionNo != null ? transactionNo : "TXN-DEP-" + bookingId + "-" + System.currentTimeMillis());
        payment.setPaymentMethod(paymentMethod != null ? paymentMethod : Payment.METHOD_VNPAY);
        payment.setAmount(booking.getDepositAmount());
        payment.setStatus(Payment.STATUS_PAID);
        payment.setPaidAt(OffsetDateTime.now());
        paymentRepository.save(payment);

        booking.setStatus("confirmed");
        bookingRepository.save(booking);

        eventPublisher.publishEvent(new DepositConfirmedEvent(bookingId));

        return payment;
    }

    @Override
    public boolean hasLiveIssuePaymentSession(Long bookingId) {
        Optional<Payment> pending = paymentRepository.findFirstByBookingIdAndPaymentTypeAndStatus(
                bookingId, Payment.TYPE_ISSUE_COMPENSATION, Payment.STATUS_PENDING);
        if (pending.isEmpty()) {
            return false;
        }
        if (isPaymentExpired(pending.get())) {
            cancelPending(pending.get());
            return false;
        }
        return true;
    }

    @Override
    public String createIssuePaymentUrl(Long bookingId, Long customerId, String clientIp) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn thuê: " + bookingId));
        if (customerId == null || !customerId.equals(booking.getCustomer().getId())) {
            throw new NoSuchElementException("Không tìm thấy đơn thuê: " + bookingId);
        }
        if (!Booking.STATUS_ACTIVE.equalsIgnoreCase(booking.getStatus())
                && !Booking.STATUS_COMPLETED.equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Chỉ có thể thanh toán phí phát sinh cho chuyến thuê đang hoạt động hoặc đã kết thúc");
        }

        // Kiểm tra lượt thanh toán pending trước đó
        Optional<Payment> existingPending = paymentRepository.findFirstByBookingIdAndPaymentTypeAndStatus(
                bookingId, Payment.TYPE_ISSUE_COMPENSATION, Payment.STATUS_PENDING);

        if (existingPending.isPresent()) {
            Payment prev = existingPending.get();
            if (!isPaymentExpired(prev)) {
                // Phiên cũ vẫn còn hiệu lực (< 15 phút), tái sử dụng để khách tiếp tục thanh toán
                return buildPaymentUrl(prev.getAmount(), Payment.ISSUE_TXN_PREFIX + prev.getId() + "_" + System.currentTimeMillis(),
                        "DriveNow_Issue_Fees_Booking_" + bookingId, clientIp);
            } else {
                // Phiên cũ đã hết hạn, huỷ để giải phóng phí
                cancelPending(prev);
            }
        }

        List<BookingFee> owed = bookingFeeService.getSettleableIssueFees(bookingId);
        if (owed.isEmpty()) {
            throw new IllegalStateException("Không có khoản phí phát sinh nào cần thanh toán");
        }
        BigDecimal total = owed.stream().map(BookingFee::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        Payment pending = new Payment();
        pending.setBooking(booking);
        pending.setPaymentType(Payment.TYPE_ISSUE_COMPENSATION);
        pending.setAmount(total);
        pending.setPaymentMethod(Payment.METHOD_VNPAY);
        pending.setStatus(Payment.STATUS_PENDING);
        pending.setTransactionCode("ISSUE-" + bookingId + "-" + System.currentTimeMillis());
        Payment saved = paymentRepository.save(pending);
        bookingFeeService.attachPayment(owed, saved);

        return buildPaymentUrl(total, Payment.ISSUE_TXN_PREFIX + saved.getId() + "_" + System.currentTimeMillis(),
                "DriveNow_Issue_Fees_Booking_" + bookingId, clientIp);
    }

    @Override
    public Payment confirmIssuePayment(Long paymentId, String transactionNo, long amountInCents) {
        Payment payment = findIssuePayment(paymentId);
        if (Payment.STATUS_PAID.equalsIgnoreCase(payment.getStatus())) {
            return payment; // Cổng có thể gọi callback nhiều lần
        }
        if (!Payment.STATUS_PENDING.equalsIgnoreCase(payment.getStatus())) {
            // Cứu giao dịch: Nếu khách đã thanh toán trên tab cũ bị đánh dấu failed nhưng phí vẫn chưa được thanh toán
            Long bookingId = payment.getBooking() != null ? payment.getBooking().getId() : null;
            List<BookingFee> owed = bookingId != null ? bookingFeeService.getSettleableIssueFees(bookingId) : List.of();
            if (!owed.isEmpty()) {
                bookingFeeService.attachPayment(owed, payment);
            } else {
                throw new IllegalStateException("Phiên thanh toán này đã hết hạn; vui lòng thực hiện lại giao dịch mới");
            }
        }
        if (payment.getAmount().multiply(new BigDecimal(100)).longValue() != amountInCents) {
            throw new IllegalArgumentException("Payment amount mismatch");
        }

        payment.setStatus(Payment.STATUS_PAID);
        payment.setPaidAt(OffsetDateTime.now());
        if (transactionNo != null && !transactionNo.isBlank()) {
            payment.setTransactionCode(transactionNo.trim());
        }
        Payment saved = paymentRepository.save(payment);
        bookingFeeService.confirmPayment(saved);
        return saved;
    }

    @Override
    public Payment failIssuePayment(Long paymentId) {
        Payment payment = findIssuePayment(paymentId);
        if (Payment.STATUS_PENDING.equalsIgnoreCase(payment.getStatus())) {
            cancelPending(payment);
        }
        return payment;
    }

    private Payment findIssuePayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy khoản thanh toán: " + paymentId));
        if (!Payment.TYPE_ISSUE_COMPENSATION.equals(payment.getPaymentType())) {
            throw new IllegalStateException("Giao dịch này không phải là thanh toán phí sự cố");
        }
        return payment;
    }

    private void cancelPending(Payment payment) {
        payment.setStatus(Payment.STATUS_FAILED);
        paymentRepository.save(payment);
        bookingFeeService.detachPayment(payment);
    }

    private boolean isPaymentExpired(Payment payment) {
        if (payment == null) {
            return false;
        }
        String code = payment.getTransactionCode();
        if (code == null) {
            return true;
        }
        int lastDash = code.lastIndexOf('-');
        if (lastDash >= 0 && lastDash < code.length() - 1) {
            try {
                long createdMillis = Long.parseLong(code.substring(lastDash + 1));
                return System.currentTimeMillis() - createdMillis > Payment.PAYMENT_TIMEOUT_MINUTES * 60 * 1000L;
            } catch (NumberFormatException ignored) {}
        }
        return true;
    }

    @Override
    public Payment recordFinalPayment(Long bookingId, BigDecimal amount, String paymentMethod) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn thuê: " + bookingId));
        Payment finalPayment = new Payment();
        finalPayment.setBooking(booking);
        finalPayment.setPaymentType("FINAL");
        finalPayment.setAmount(amount);
        finalPayment.setStatus("paid");
        finalPayment.setPaymentMethod(paymentMethod);
        finalPayment.setTransactionCode("FINAL-" + bookingId + "-" + System.currentTimeMillis());
        finalPayment.setPaidAt(OffsetDateTime.now());
        return paymentRepository.save(finalPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByBooking(Long bookingId) {
        return paymentRepository.findByBookingIdOrderByPaidAtAsc(bookingId);
    }

    @Override
    @Transactional
    public Payment createPendingRefund(Long bookingId, BigDecimal amount, String bankCode, String accountNumber,
            String accountHolder) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn thuê: " + bookingId));

        Payment refund = new Payment();
        refund.setBooking(booking);
        refund.setPaymentType(Payment.TYPE_REFUND);
        refund.setAmount(amount != null ? amount
                : (booking.getDepositAmount() != null ? booking.getDepositAmount() : BigDecimal.ZERO));
        refund.setPaymentMethod(Payment.METHOD_BANK_TRANSFER);
        refund.setStatus(Payment.STATUS_PENDING);

        String cleanBank = (bankCode != null && !bankCode.isBlank()) ? bankCode.trim().toUpperCase() : "BANK";
        String cleanAcc = (accountNumber != null && !accountNumber.isBlank()) ? accountNumber.trim() : "";
        String cleanHolder = (accountHolder != null && !accountHolder.isBlank()) ? accountHolder.trim().toUpperCase()
                : "";
        String refInfo = cleanBank + ": " + cleanAcc + " - " + cleanHolder;
        if (refInfo.length() > 100) {
            refInfo = refInfo.substring(0, 100);
        }
        refund.setTransactionCode(refInfo);

        return paymentRepository.save(refund);
    }

    @Override
    @Transactional
    public Payment confirmRefundCompleted(Long paymentId, Long employeeId, String transactionNo) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy khoản thanh toán: " + paymentId));

        if (!Payment.TYPE_REFUND.equalsIgnoreCase(payment.getPaymentType())) {
            throw new IllegalStateException("Giao dịch không phải là khoản hoàn tiền");
        }
        if (!Payment.STATUS_PENDING.equalsIgnoreCase(payment.getStatus())) {
            return payment; // Idempotent
        }

        payment.setStatus(Payment.STATUS_PAID);
        payment.setPaidAt(OffsetDateTime.now());
        if (transactionNo != null && !transactionNo.isBlank()) {
            payment.setTransactionCode(transactionNo.trim());
        }

        Payment saved = paymentRepository.save(payment);
        eventPublisher.publishEvent(new RefundCompletedEvent(payment.getBooking().getId(), payment.getId()));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> getPendingRefund(Long bookingId) {
        return paymentRepository.findFirstByBookingIdAndPaymentTypeAndStatus(bookingId, Payment.TYPE_REFUND, Payment.STATUS_PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> getPendingRefunds() {
        return paymentRepository.findByPaymentTypeAndStatus(Payment.TYPE_REFUND, Payment.STATUS_PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public long countPendingRefunds() {
        return paymentRepository.countPendingRefunds();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getPendingRefundBookingIds() {
        return paymentRepository.findBookingIdsWithPendingRefund();
    }
}
