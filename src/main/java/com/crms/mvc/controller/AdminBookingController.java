package com.crms.mvc.controller;

import com.crms.mvc.util.CurrentAccount;
import com.crms.mvc.dto.BookingDetailDTO;
import com.crms.mvc.dto.BookingSummaryDTO;
import com.crms.mvc.dto.CheckOutDTO;
import com.crms.mvc.dto.CheckInDTO;
import com.crms.mvc.dto.SettlementDTO;
import com.crms.mvc.service.HandoverService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.server.ResponseStatusException;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.service.BookingService;
import com.crms.mvc.service.VehicleService;
import com.crms.mvc.entity.Payment;
import com.crms.mvc.service.IssueService;
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
    private final IssueService issueService;
    private final HandoverService handoverService;

    public AdminBookingController(BookingService bookingService, VehicleService vehicleService,
                                  PaymentService paymentService, IssueService issueService,
                                  HandoverService handoverService) {
        this.bookingService = bookingService;
        this.vehicleService = vehicleService;
        this.paymentService = paymentService;
        this.issueService = issueService;
        this.handoverService = handoverService;
    }

    @GetMapping
    public String listBookings(@RequestParam(value = "status", required = false, defaultValue = "all") String status,
                               HttpSession session, Model model, RedirectAttributes ra) {
        Long employeeId = CurrentAccount.employeeId(session).orElse(null);
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
        Long employeeId = CurrentAccount.employeeId(session).orElse(null);
        if (employeeId == null) return "redirect:/login";

        try {
            BookingDetailDTO detail = bookingService.getBookingDetailForAdmin(bookingId);
            List<Vehicle> availableVehicles = vehicleService.findByStatusWithDetails(Vehicle.STATUS_AVAILABLE).stream()
                    .filter(v -> v.getVehicleType().getId().equals(detail.getVehicleTypeId()))
                    .toList();

            model.addAttribute("booking", detail);
            model.addAttribute("availableVehicles", availableVehicles);
            model.addAttribute("checkInForm", new CheckInDTO());
            model.addAttribute("checkOutForm", new CheckOutDTO());
            model.addAttribute("issues", issueService.findByBooking(bookingId));
            model.addAttribute("settlementForm", new SettlementDTO());
            model.addAttribute("handover", handoverService.getSummary(bookingId));
            model.addAttribute("isAdminAccount", CurrentAccount.isAdmin(session));
            model.addAttribute("canHandle", CurrentAccount.isAdmin(session) || employeeId.equals(detail.getStaffId()));
            if (CurrentAccount.isAdmin(session)) {
                model.addAttribute("assignableEmployees", handoverService.findAssignableEmployees());
            }

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
        Long employeeId = CurrentAccount.employeeId(session).orElse(null);
        if (employeeId == null) return "redirect:/login";

        try {
            bookingService.assignVehicle(bookingId, vehicleId);
            ra.addFlashAttribute("success", "Vehicle assigned successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Assignment failed: " + e.getMessage());
        }
        return "redirect:/admin/bookings/" + bookingId;
    }

    @PostMapping("/{id}/assign-staff")
    public String assignStaff(@PathVariable("id") Long bookingId, @RequestParam("staffId") Long staffId,
                              HttpSession session, RedirectAttributes ra) {
        if (!CurrentAccount.isAdmin(session)) {
            ra.addFlashAttribute("error", "Only an admin can change the assigned staff.");
            return "redirect:/admin/bookings/" + bookingId;
        }

        try {
            handoverService.reassignStaff(bookingId, staffId);
            ra.addFlashAttribute("success", "Assigned staff updated.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Could not change assigned staff: " + e.getMessage());
        }
        return "redirect:/admin/bookings/" + bookingId;
    }

    @PostMapping("/{id}/check-in")
    public String checkIn(@PathVariable("id") Long bookingId,
                          @Valid @ModelAttribute("checkInForm") CheckInDTO form, BindingResult result,
                          HttpSession session, RedirectAttributes ra) {
        Long employeeId = CurrentAccount.employeeId(session).orElse(null);
        if (employeeId == null) return "redirect:/login";

        if (result.hasErrors()) {
            ra.addFlashAttribute("error", "Check-in failed: " + result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/bookings/" + bookingId;
        }

        try {
            handoverService.checkIn(bookingId, employeeId, form);
            ra.addFlashAttribute("success", "Check-in completed. Rental trip is now ACTIVE.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Check-in failed: " + e.getMessage());
        }
        return "redirect:/admin/bookings/" + bookingId;
    }

    @PostMapping("/{id}/check-out")
    public String checkOut(@PathVariable("id") Long bookingId,
                           @Valid @ModelAttribute("checkOutForm") CheckOutDTO form, BindingResult result,
                           HttpSession session, RedirectAttributes ra) {
        Long employeeId = CurrentAccount.employeeId(session).orElse(null);
        if (employeeId == null) return "redirect:/login";

        if (result.hasErrors()) {
            ra.addFlashAttribute("error", "Check-out failed: " + result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/bookings/" + bookingId;
        }

        try {
            handoverService.checkOut(bookingId, employeeId, form);
            ra.addFlashAttribute("success", "Vehicle return recorded. Complete the settlement to close this booking.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Check-out failed: " + e.getMessage());
        }
        return "redirect:/admin/bookings/" + bookingId;
    }

    @PostMapping("/{id}/settle")
    public String settle(@PathVariable("id") Long bookingId,
                         @Valid @ModelAttribute("settlementForm") SettlementDTO form, BindingResult result,
                         HttpSession session, RedirectAttributes ra) {
        Long employeeId = CurrentAccount.employeeId(session).orElse(null);
        if (employeeId == null) return "redirect:/login";

        if (result.hasErrors()) {
            ra.addFlashAttribute("error", "Settlement failed: " + result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/bookings/" + bookingId;
        }

        try {
            handoverService.settle(bookingId, employeeId, form);
            ra.addFlashAttribute("success", "Settlement completed. This booking is now closed.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Settlement failed: " + e.getMessage());
        }
        return "redirect:/admin/bookings/" + bookingId;
    }

    @GetMapping("/{id}/records/{recordId}/evidence")
    public ResponseEntity<Resource> evidence(@PathVariable("id") Long bookingId,
                                             @PathVariable("recordId") Long recordId,
                                             @RequestParam(value = "secondary", defaultValue = "false") boolean secondary) {
        Resource image = handoverService.loadEvidence(bookingId, recordId, secondary);
        if (image == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok()
                .contentType(MediaTypeFactory.getMediaType(image).orElse(MediaType.APPLICATION_OCTET_STREAM))
                .cacheControl(CacheControl.noStore())
                .body(image);
    }

    @PostMapping("/{id}/refund/{paymentId}/confirm")
    public String confirmRefund(@PathVariable("id") Long bookingId,
                                @PathVariable("paymentId") Long paymentId,
                                @RequestParam(value = "transactionNo", required = false) String transactionNo,
                                HttpSession session, RedirectAttributes ra) {
        Long employeeId = CurrentAccount.employeeId(session).orElse(null);
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
