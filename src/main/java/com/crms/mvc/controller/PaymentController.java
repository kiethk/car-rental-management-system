package com.crms.mvc.controller;

import com.crms.mvc.enums.PaymentMethod;

import com.crms.mvc.util.EnumTokens;
import com.crms.mvc.enums.BookingStatus;
import com.crms.mvc.util.CurrentAccount;
import com.crms.mvc.config.VnPayConfig;
import com.crms.mvc.dto.DepositSessionDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Payment;
import com.crms.mvc.service.BookingService;
import com.crms.mvc.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Map;
import java.util.NoSuchElementException;
import java.time.OffsetDateTime;

@Controller
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private static final String ISSUE_REFERENCE_PREFIX = Payment.ISSUE_TXN_PREFIX;

    private final PaymentService paymentService;
    private final BookingService bookingService;

    public PaymentController(PaymentService paymentService, BookingService bookingService) {
        this.paymentService = paymentService;
        this.bookingService = bookingService;
    }

    @PostMapping("/bookings/{id}/pay-deposit")
    public String initiateDepositPayment(@PathVariable("id") Long bookingId,
                                         @RequestParam(value = "method", defaultValue = "VNPAY") String method,
                                         HttpServletRequest request,
                                         HttpSession session,
                                         RedirectAttributes ra) {
        Long customerId = CurrentAccount.customerId(session).orElse(null);
        if (customerId == null) {
            return "redirect:/login";
        }

        DepositSessionDTO deposit = paymentService.findDepositSession(bookingId).orElse(null);
        if (deposit == null) {
            ra.addFlashAttribute("error", "Không tìm thấy đơn đặt xe.");
            return "redirect:/bookings/my-bookings";
        }

        if (!customerId.equals(deposit.customerId())) {
            ra.addFlashAttribute("error", "Bạn không có quyền thanh toán đơn đặt xe này.");
            return "redirect:/bookings/my-bookings";
        }

        if (EnumTokens.is(BookingStatus.CONFIRMED, deposit.status())) {
            return "redirect:/bookings/" + bookingId + "/success";
        }

        if (!EnumTokens.is(BookingStatus.PENDING_PAYMENT, deposit.status())) {
            ra.addFlashAttribute("error", "Đơn đặt xe không ở trạng thái chờ đặt cọc (Trạng thái hiện tại: " + deposit.status() + ").");
            return "redirect:/bookings/" + bookingId;
        }

        // Hạn chót 15 phút: tự động hủy đơn và chặn thanh toán nếu quá 15 phút
        if (deposit.createdAt() != null && deposit.createdAt().plusMinutes(Payment.PAYMENT_TIMEOUT_MINUTES).isBefore(OffsetDateTime.now())) {
            bookingService.cancelUnpaidBooking(bookingId, customerId);
            ra.addFlashAttribute("error", "Đơn đặt xe đã hết hạn thanh toán cọc sau 15 phút và đã tự động hủy.");
            return "redirect:/bookings/" + bookingId;
        }

        try {
            String clientIp = VnPayConfig.getIpAddress(request);
            String paymentUrl = paymentService.createDepositPaymentUrl(bookingId, clientIp);
            return "redirect:" + paymentUrl;
        } catch (Exception e) {
            log.error("Failed to initiate payment for booking {}", bookingId, e);
            ra.addFlashAttribute("error", "Không thể kết nối cổng thanh toán: " + e.getMessage());
            return "redirect:/bookings/" + bookingId + "/pay-deposit";
        }
    }

    /** Pays every outstanding issue fee of a completed booking in one go; the amount is computed, never typed. */
    @PostMapping("/bookings/{id}/pay-issue-fees")
    public String initiateIssuePayment(@PathVariable("id") Long bookingId,
                                       HttpServletRequest request,
                                       HttpSession session,
                                       RedirectAttributes ra) {
        Long customerId = CurrentAccount.customerId(session).orElse(null);
        if (customerId == null) {
            return "redirect:/login";
        }
        try {
            String paymentUrl = paymentService.createIssuePaymentUrl(bookingId, customerId, VnPayConfig.getIpAddress(request));
            return "redirect:" + paymentUrl;
        } catch (IllegalStateException | NoSuchElementException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/bookings/" + bookingId;
        }
    }

    @GetMapping("/payment/{gateway}/return")
    public String handlePaymentReturn(@PathVariable("gateway") String gatewayPath,
                                      @RequestParam Map<String, String> allParams,
                                      RedirectAttributes ra) {
        String gateway = (gatewayPath != null && !gatewayPath.isBlank()) ? gatewayPath.toUpperCase() : "VNPAY";

        String vnp_TxnRef = allParams.get("vnp_TxnRef");
        String responseCode = allParams.get("vnp_ResponseCode");
        String transactionNo = allParams.get("vnp_TransactionNo");
        String amountStr = allParams.get("vnp_Amount");

        log.info("Received {} callback: TxnRef={}, ResponseCode={}, TxnNo={}", gateway, vnp_TxnRef, responseCode, transactionNo);

        if (vnp_TxnRef == null || vnp_TxnRef.isEmpty()) {
            ra.addFlashAttribute("error", "Phản hồi thanh toán không hợp lệ: thiếu mã giao dịch.");
            return "redirect:/";
        }

        if (vnp_TxnRef.startsWith(ISSUE_REFERENCE_PREFIX)) {
            return handleIssuePaymentReturn(vnp_TxnRef, allParams, ra);
        }

        Long bookingId;
        try {
            String[] parts = vnp_TxnRef.split("_");
            bookingId = Long.parseLong(parts[0]);
        } catch (Exception e) {
            log.error("Invalid vnp_TxnRef format: {}", vnp_TxnRef, e);
            ra.addFlashAttribute("error", "Mã giao dịch không đúng định dạng.");
            return "redirect:/";
        }

        boolean isValidSignature = paymentService.isValidGatewayReturn(allParams);
        if (!isValidSignature) {
            log.error("{} signature verification failed for TxnRef: {}", gateway, vnp_TxnRef);
            ra.addFlashAttribute("error", "Xác minh thanh toán thất bại: chữ ký số không hợp lệ.");
            return "redirect:/bookings/" + bookingId + "/pay-deposit";
        }

        if ("00".equals(responseCode)) {
            try {
                DepositSessionDTO deposit = paymentService.findDepositSession(bookingId).orElse(null);
                if (deposit == null) {
                    ra.addFlashAttribute("error", "Không tìm thấy đơn đặt xe: " + bookingId);
                    return "redirect:/";
                }

                if (!EnumTokens.is(BookingStatus.PENDING_PAYMENT, deposit.status()) && !EnumTokens.is(BookingStatus.CONFIRMED, deposit.status())) {
                    ra.addFlashAttribute("error", "Đơn đặt xe không ở trạng thái chờ thanh toán (trạng thái: " + deposit.status() + ").");
                    return "redirect:/bookings/" + bookingId;
                }

                // Verify returned amount (amount is in cents, multiplied by 100)
                if (amountStr != null) {
                    long returnedAmount = Long.parseLong(amountStr);
                    long expectedAmount = deposit.depositAmount().multiply(BigDecimal.valueOf(100)).longValue();
                    if (returnedAmount != expectedAmount) {
                        log.error("Payment amount mismatch for booking {}: expected={}, received={}", bookingId, expectedAmount, returnedAmount);
                        ra.addFlashAttribute("error", "Số tiền thanh toán không khớp.");
                        return "redirect:/bookings/" + bookingId + "/pay-deposit";
                    }
                }

                paymentService.confirmDeposit(bookingId, transactionNo, PaymentMethod.from(gateway));
                log.info("Successfully processed deposit for booking {} via {} (Txn: {})", bookingId, gateway, transactionNo);
                return "redirect:/bookings/" + bookingId + "/success?vnp_TxnRef=" + vnp_TxnRef +
                        (transactionNo != null ? "&vnp_TransactionNo=" + transactionNo : "");
            } catch (Exception e) {
                log.error("Error finalizing deposit for booking {}", bookingId, e);
                ra.addFlashAttribute("error", "Cổng thanh toán đã ghi nhận nhưng không cập nhật được đơn đặt xe: " + e.getMessage());
                return "redirect:/bookings/" + bookingId + "/pay-deposit";
            }
        } else if ("24".equals(responseCode)) {
            ra.addFlashAttribute("error", "Bạn đã hủy thanh toán. Bạn có thể thử lại hoặc chọn phương thức thanh toán khác.");
            return "redirect:/bookings/" + bookingId + "/pay-deposit";
        } else {
            ra.addFlashAttribute("error", "Payment failed (Code " + responseCode + "). Please try again.");
            return "redirect:/bookings/" + bookingId + "/pay-deposit";
        }
    }

    /** Return from the gateway for fees owed after settlement (reference "I{paymentId}_{time}"). */
    private String handleIssuePaymentReturn(String txnRef, Map<String, String> params, RedirectAttributes ra) {
        if (!paymentService.isValidGatewayReturn(params)) {
            log.error("Signature verification failed for issue payment reference {}", txnRef);
            ra.addFlashAttribute("error", "Xác minh thanh toán thất bại: chữ ký số không hợp lệ.");
            return "redirect:/bookings/my-bookings";
        }
        try {
            Long paymentId = Long.parseLong(txnRef.substring(ISSUE_REFERENCE_PREFIX.length()).split("_")[0]);
            Payment payment;
            if ("00".equals(params.get("vnp_ResponseCode"))) {
                long amountInCents = Long.parseLong(params.getOrDefault("vnp_Amount", "0"));
                payment = paymentService.confirmIssuePayment(paymentId, params.get("vnp_TransactionNo"), amountInCents);
                ra.addFlashAttribute("success", "Thanh toán phí phát sinh thành công. Cảm ơn bạn!");
            } else {
                payment = paymentService.failIssuePayment(paymentId);
                ra.addFlashAttribute("error", "Thanh toán chưa hoàn tất (mã " + params.get("vnp_ResponseCode")
                        + "). Khoản phí vẫn chưa được thanh toán, bạn có thể thử lại.");
            }
            return "redirect:/bookings/" + payment.getBooking().getId();
        } catch (IllegalStateException | IllegalArgumentException | NoSuchElementException e) {
            log.error("Could not record issue payment {}", txnRef, e);
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/bookings/my-bookings";
        }
    }
}
