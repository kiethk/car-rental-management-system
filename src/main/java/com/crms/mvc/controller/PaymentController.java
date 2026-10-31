package com.crms.mvc.controller;

import com.crms.mvc.entity.Booking;
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

@Controller
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

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
        Long customerId = (Long) session.getAttribute("customerId");
        if (customerId == null) {
            return "redirect:/login";
        }

        Booking booking = bookingService.findEntityById(bookingId).orElse(null);
        if (booking == null) {
            ra.addFlashAttribute("error", "Booking not found.");
            return "redirect:/bookings/my-bookings";
        }

        if (booking.getCustomer() == null || !booking.getCustomer().getId().equals(customerId)) {
            ra.addFlashAttribute("error", "You do not have permission to pay for this booking.");
            return "redirect:/bookings/my-bookings";
        }

        if ("confirmed".equalsIgnoreCase(booking.getStatus())) {
            return "redirect:/bookings/" + bookingId + "/success";
        }

        if (!"pending_payment".equalsIgnoreCase(booking.getStatus())) {
            ra.addFlashAttribute("error", "Đơn đặt xe không ở trạng thái chờ đặt cọc (Trạng thái hiện tại: " + booking.getStatus() + ").");
            return "redirect:/bookings/" + bookingId;
        }

        // Hạn chót 15 phút: tự động hủy đơn và chặn thanh toán nếu quá 15 phút
        if (booking.getCreatedAt() != null && booking.getCreatedAt().plusMinutes(15).isBefore(java.time.OffsetDateTime.now())) {
            bookingService.cancelBooking(bookingId, customerId);
            ra.addFlashAttribute("error", "Đơn đặt xe đã hết hạn thanh toán cọc sau 15 phút và đã tự động hủy.");
            return "redirect:/bookings/" + bookingId;
        }

        try {
            String paymentUrl = paymentService.createDepositPaymentUrl(booking, request);
            return "redirect:" + paymentUrl;
        } catch (Exception e) {
            log.error("Failed to initiate payment for booking {}", bookingId, e);
            ra.addFlashAttribute("error", "Could not connect to payment gateway: " + e.getMessage());
            return "redirect:/bookings/" + bookingId + "/pay-deposit";
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
            ra.addFlashAttribute("error", "Invalid payment response: missing transaction reference.");
            return "redirect:/";
        }

        Long bookingId;
        try {
            String[] parts = vnp_TxnRef.split("_");
            bookingId = Long.parseLong(parts[0]);
        } catch (Exception e) {
            log.error("Invalid vnp_TxnRef format: {}", vnp_TxnRef, e);
            ra.addFlashAttribute("error", "Invalid transaction reference format.");
            return "redirect:/";
        }

        boolean isValidSignature = paymentService.verifyPaymentSignature(allParams);
        if (!isValidSignature) {
            log.error("{} signature verification failed for TxnRef: {}", gateway, vnp_TxnRef);
            ra.addFlashAttribute("error", "Payment verification failed: invalid digital signature.");
            return "redirect:/bookings/" + bookingId + "/pay-deposit";
        }

        if ("00".equals(responseCode)) {
            try {
                Booking booking = bookingService.findEntityById(bookingId).orElse(null);
                if (booking == null) {
                    ra.addFlashAttribute("error", "Booking not found: " + bookingId);
                    return "redirect:/";
                }

                if (!"pending_payment".equalsIgnoreCase(booking.getStatus()) && !"confirmed".equalsIgnoreCase(booking.getStatus())) {
                    ra.addFlashAttribute("error", "Booking is not in pending_payment status (status: " + booking.getStatus() + ").");
                    return "redirect:/bookings/" + bookingId;
                }

                // Verify returned amount (amount is in cents, multiplied by 100)
                if (amountStr != null) {
                    long returnedAmount = Long.parseLong(amountStr);
                    long expectedAmount = booking.getDepositAmount().multiply(BigDecimal.valueOf(100)).longValue();
                    if (returnedAmount != expectedAmount) {
                        log.error("Payment amount mismatch for booking {}: expected={}, received={}", bookingId, expectedAmount, returnedAmount);
                        ra.addFlashAttribute("error", "Payment amount mismatch.");
                        return "redirect:/bookings/" + bookingId + "/pay-deposit";
                    }
                }

                paymentService.processDeposit(bookingId, transactionNo, gateway);
                log.info("Successfully processed deposit for booking {} via {} (Txn: {})", bookingId, gateway, transactionNo);
                return "redirect:/bookings/" + bookingId + "/success?vnp_TxnRef=" + vnp_TxnRef +
                        (transactionNo != null ? "&vnp_TransactionNo=" + transactionNo : "");
            } catch (Exception e) {
                log.error("Error finalizing deposit for booking {}", bookingId, e);
                ra.addFlashAttribute("error", "Payment succeeded on gateway but failed to update booking: " + e.getMessage());
                return "redirect:/bookings/" + bookingId + "/pay-deposit";
            }
        } else if ("24".equals(responseCode)) {
            ra.addFlashAttribute("error", "Payment was cancelled. You can try again or select another payment method.");
            return "redirect:/bookings/" + bookingId + "/pay-deposit";
        } else {
            ra.addFlashAttribute("error", "Payment failed (Code " + responseCode + "). Please try again.");
            return "redirect:/bookings/" + bookingId + "/pay-deposit";
        }
    }
}

