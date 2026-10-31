package com.crms.mvc.controller;

import com.crms.mvc.dto.BookingDetailDTO;
import com.crms.mvc.dto.BookingSummaryDTO;
import com.crms.mvc.dto.CheckOutDTO;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.service.BookingService;
import com.crms.mvc.service.VehicleService;
import com.crms.mvc.entity.Payment;
import com.crms.mvc.service.PaymentService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.VehicleType;
import com.crms.mvc.util.DateTimeUtils;

@Controller
@RequestMapping("/admin/bookings")
public class AdminBookingController {
    private final BookingService bookingService;
    private final VehicleService vehicleService;
    private final PaymentService paymentService;

    public AdminBookingController(BookingService bookingService, VehicleService vehicleService, PaymentService paymentService) {
        this.bookingService = bookingService;
        this.vehicleService = vehicleService;
        this.paymentService = paymentService;
    }

    @GetMapping
    public String listBookings(@RequestParam(value = "status", required = false, defaultValue = "all") String status,
                               HttpSession session, Model model, RedirectAttributes ra) {
        Long employeeId = (Long) session.getAttribute("employeeId");
        if (employeeId == null) {
            ra.addFlashAttribute("error", "Admin / Staff authentication required.");
            return "redirect:/login";
        }

        long pendingRefundCount = paymentService.countPendingRefunds();
        model.addAttribute("pendingRefundCount", pendingRefundCount);

        List<BookingSummaryDTO> list = bookingService.getAllBookings(status);
        model.addAttribute("bookings", list);
        model.addAttribute("currentStatus", status);
        return "admin/bookings/list";
    }


    @GetMapping("/{id}")
    public String manageBooking(@PathVariable("id") Long bookingId, HttpSession session, Model model, RedirectAttributes ra) {
        Long employeeId = (Long) session.getAttribute("employeeId");
        if (employeeId == null) return "redirect:/login";

        try {
            BookingDetailDTO detail = bookingService.getBookingDetailForAdmin(bookingId);
            List<Vehicle> availableVehicles = vehicleService.findByStatusWithDetails("available").stream()
                    .filter(v -> v.getVehicleType().getId().equals(detail.getVehicleTypeId()))
                    .toList();

            model.addAttribute("booking", detail);
            model.addAttribute("availableVehicles", availableVehicles);
            model.addAttribute("checkOutForm", new CheckOutDTO());

            Optional<Payment> pendingRefundOpt = paymentService.getPendingRefund(bookingId);
            if (pendingRefundOpt.isPresent()) {
                Payment refund = pendingRefundOpt.get();
                model.addAttribute("pendingRefund", refund);

                String raw = refund.getTransactionCode() != null ? refund.getTransactionCode() : "";
                String bankCode = "";
                String accountNumber = "";
                String accountHolder = "";

                if (raw.contains(":")) {
                    String[] parts = raw.split(":", 2);
                    bankCode = parts[0].trim();
                    String rest = parts[1].trim();
                    if (rest.contains("-")) {
                        String[] accParts = rest.split("-", 2);
                        accountNumber = accParts[0].trim();
                        accountHolder = accParts[1].trim();
                    } else {
                        accountNumber = rest;
                    }
                }

                model.addAttribute("refundBankCode", bankCode);
                model.addAttribute("refundAccountNumber", accountNumber);
                model.addAttribute("refundAccountHolder", accountHolder);

                String addInfo = "DriveNow HoanCoc DH" + bookingId;
                String qrUrl = String.format("https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
                        bankCode, accountNumber, refund.getAmount().longValue(),
                        URLEncoder.encode(addInfo, StandardCharsets.UTF_8),
                        URLEncoder.encode(accountHolder, StandardCharsets.UTF_8));
                model.addAttribute("vietQrUrl", qrUrl);
            }

            return "admin/bookings/manage";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/bookings";
        }
    }

    @PostMapping("/{id}/assign")
    public String assignVehicle(@PathVariable("id") Long bookingId, @RequestParam("vehicleId") Long vehicleId,
                                HttpSession session, RedirectAttributes ra) {
        Long employeeId = (Long) session.getAttribute("employeeId");
        if (employeeId == null) return "redirect:/login";

        try {
            bookingService.assignVehicle(bookingId, vehicleId);
            ra.addFlashAttribute("success", "Vehicle assigned successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Assignment failed: " + e.getMessage());
        }
        return "redirect:/admin/bookings/" + bookingId;
    }

    @PostMapping("/{id}/check-in")
    public String checkIn(@PathVariable("id") Long bookingId, HttpSession session, RedirectAttributes ra) {
        Long employeeId = (Long) session.getAttribute("employeeId");
        if (employeeId == null) return "redirect:/login";

        try {
            bookingService.checkIn(bookingId, employeeId);
            ra.addFlashAttribute("success", "Check-in completed. Rental trip is now ACTIVE.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Check-in failed: " + e.getMessage());
        }
        return "redirect:/admin/bookings/" + bookingId;
    }

    @PostMapping("/{id}/check-out")
    public String checkOut(@PathVariable("id") Long bookingId, @ModelAttribute("checkOutForm") CheckOutDTO form,
                           HttpSession session, RedirectAttributes ra) {
        Long employeeId = (Long) session.getAttribute("employeeId");
        if (employeeId == null) return "redirect:/login";

        try {
            bookingService.checkOut(bookingId, employeeId, form);
            ra.addFlashAttribute("success", "Check-out completed. Settlement & deposit refund processed.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Check-out failed: " + e.getMessage());
        }
        return "redirect:/admin/bookings/" + bookingId;
    }

    @PostMapping("/{id}/refund/{paymentId}/confirm")
    public String confirmRefund(@PathVariable("id") Long bookingId,
                                @PathVariable("paymentId") Long paymentId,
                                @RequestParam(value = "transactionNo", required = false) String transactionNo,
                                HttpSession session, RedirectAttributes ra) {
        Long employeeId = (Long) session.getAttribute("employeeId");
        if (employeeId == null) return "redirect:/login";

        try {
            paymentService.confirmRefundCompleted(paymentId, employeeId, transactionNo);
            ra.addFlashAttribute("success", "Xác nhận đã hoàn tiền cọc thành công! Email thông báo giao dịch hoàn tất đã được gửi cho khách hàng.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi xác nhận hoàn cọc: " + e.getMessage());
        }
        return "redirect:/admin/bookings/" + bookingId;
    }
}
