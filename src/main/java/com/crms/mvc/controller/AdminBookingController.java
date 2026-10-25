package com.crms.mvc.controller;

import com.crms.mvc.dto.BookingDetailDTO;
import com.crms.mvc.dto.BookingSummaryDTO;
import com.crms.mvc.dto.CheckOutDTO;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.service.BookingService;
import com.crms.mvc.service.VehicleService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequestMapping("/admin/bookings")
public class AdminBookingController {
    private final BookingService bookingService;
    private final VehicleService vehicleService;

    public AdminBookingController(BookingService bookingService, VehicleService vehicleService) {
        this.bookingService = bookingService;
        this.vehicleService = vehicleService;
    }

    @GetMapping
    public String listBookings(@RequestParam(value = "status", required = false, defaultValue = "all") String status,
                               HttpSession session, Model model, RedirectAttributes ra) {
        Long employeeId = (Long) session.getAttribute("employeeId");
        if (employeeId == null) {
            ra.addFlashAttribute("error", "Admin / Staff authentication required.");
            return "redirect:/login";
        }

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
}
