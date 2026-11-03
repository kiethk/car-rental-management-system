package com.crms.mvc.controller;

import com.crms.mvc.entity.Booking;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.service.PaymentService;
import com.crms.mvc.service.VnPayService;
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

import java.util.Map;
import java.util.NoSuchElementException;

@Controller
public class VnPayController {

    private static final Logger log = LoggerFactory.getLogger(VnPayController.class);

    private final VnPayService vnPayService;
    private final PaymentService paymentService;
    private final BookingRepository bookingRepository;

    public VnPayController(VnPayService vnPayService, PaymentService paymentService, BookingRepository bookingRepository) {
        this.vnPayService = vnPayService;
        this.paymentService = paymentService;
        this.bookingRepository = bookingRepository;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    @org.springframework.web.bind.annotation.RequestMapping(value = "/bookings/{id}/vnpay-payment", method = {org.springframework.web.bind.annotation.RequestMethod.POST, org.springframework.web.bind.annotation.RequestMethod.GET})
    public String initiateVnPayPayment(@PathVariable("id") Long bookingId,
                                      HttpServletRequest request,
                                      HttpSession session,
                                      RedirectAttributes ra) {
        Long customerId = (Long) session.getAttribute("customerId");
        if (customerId == null) {
            return "redirect:/login";
        }

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
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
            ra.addFlashAttribute("error", "Booking status does not allow deposit payment (status: " + booking.getStatus() + ").");
            return "redirect:/bookings/" + bookingId;
        }

        try {
            String paymentUrl = vnPayService.createDepositPaymentUrl(booking, request);
            return "redirect:" + paymentUrl;
        } catch (Exception e) {
            log.error("Failed to initiate VNPay payment for booking {}", bookingId, e);
            ra.addFlashAttribute("error", "Could not connect to VNPay gateway: " + e.getMessage());
            return "redirect:/bookings/" + bookingId + "/pay-deposit";
        }
    }

    @GetMapping("/payment/vnpay-return")
    public String handleVnPayReturn(@RequestParam Map<String, String> allParams,
                                    RedirectAttributes ra) {
        log.info("Received VNPay callback params: {}", allParams);

        String vnp_TxnRef = allParams.get("vnp_TxnRef");
        if (vnp_TxnRef == null || vnp_TxnRef.isEmpty()) {
            ra.addFlashAttribute("error", "Invalid VNPay payment response: missing transaction reference.");
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

        boolean isValidSignature = vnPayService.verifyPaymentSignature(allParams);
        if (!isValidSignature) {
            log.error("VNPay signature verification failed for vnp_TxnRef: {}", vnp_TxnRef);
            ra.addFlashAttribute("error", "Payment verification failed: invalid digital signature.");
            return "redirect:/bookings/" + bookingId + "/pay-deposit";
        }

        String responseCode = allParams.get("vnp_ResponseCode");
        String transactionNo = allParams.get("vnp_TransactionNo");

        if ("00".equals(responseCode)) {
            try {
                paymentService.processDeposit(bookingId);
                log.info("Successfully processed deposit for booking {} via VNPay (Txn: {})", bookingId, transactionNo);
                return "redirect:/bookings/" + bookingId + "/success?vnp_TxnRef=" + vnp_TxnRef + 
                       (transactionNo != null ? "&vnp_TransactionNo=" + transactionNo : "");
            } catch (Exception e) {
                log.error("Error finalizing deposit for booking {}", bookingId, e);
                ra.addFlashAttribute("error", "Payment succeeded on VNPay but failed to update booking: " + e.getMessage());
                return "redirect:/bookings/" + bookingId + "/pay-deposit";
            }
        } else if ("24".equals(responseCode)) {
            ra.addFlashAttribute("error", "VNPay payment was cancelled. You can try again or select another payment method.");
            return "redirect:/bookings/" + bookingId + "/pay-deposit";
        } else {
            ra.addFlashAttribute("error", "VNPay payment failed (Code " + responseCode + "). Please try again.");
            return "redirect:/bookings/" + bookingId + "/pay-deposit";
        }
    }
}

