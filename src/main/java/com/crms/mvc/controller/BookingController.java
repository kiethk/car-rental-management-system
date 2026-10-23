package com.crms.mvc.controller;

import com.crms.mvc.dto.BookingCreateDTO;
import com.crms.mvc.dto.BookingDetailDTO;
import com.crms.mvc.dto.BookingSummaryDTO;
import com.crms.mvc.dto.VehicleTypeSearchResultDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.entity.VehicleType;
import com.crms.mvc.repository.VehicleTypeRepository;
import com.crms.mvc.service.BookingService;
import com.crms.mvc.service.PaymentService;
import com.crms.mvc.service.SlotService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import com.crms.mvc.entity.LicenseDocument;
import com.crms.mvc.service.LicenseDocumentService;

@Controller
public class BookingController {
    private final BookingService bookingService;
    private final SlotService slotService;
    private final PaymentService paymentService;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final LicenseDocumentService licenseDocumentService;

    public BookingController(
            BookingService bookingService,
            SlotService slotService,
            PaymentService paymentService,
            VehicleTypeRepository vehicleTypeRepository,
            LicenseDocumentService licenseDocumentService) {
        this.bookingService = bookingService;
        this.slotService = slotService;
        this.paymentService = paymentService;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.licenseDocumentService = licenseDocumentService;
    }

    // STEP 1 & 2: Choose rental schedule & filter available vehicle types
    @GetMapping({"/search", "/vehicles/available"})
    public String searchAvailableVehicles(
            @RequestParam(value = "startDate", required = false) String startDateStr,
            @RequestParam(value = "endDate", required = false) String endDateStr,
            @RequestParam(value = "date", required = false) String dateStr,
            @RequestParam(value = "startTime", required = false) String startStr,
            @RequestParam(value = "endTime", required = false) String endStr,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "brand", required = false, defaultValue = "all") String brand,
            @RequestParam(value = "category", required = false, defaultValue = "all") String category,
            @RequestParam(value = "seats", required = false, defaultValue = "0") Integer minSeats,
            @RequestParam(value = "transmission", required = false, defaultValue = "all") String transmission,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "sortBy", required = false, defaultValue = "price") String sortBy,
            Model model) {

        LocalDate startDate = LocalDate.now().plusDays(1);
        if (startDateStr != null && !startDateStr.isBlank()) {
            try { startDate = LocalDate.parse(startDateStr.trim()); } catch (Exception ignored) {}
        } else if (dateStr != null && !dateStr.isBlank()) {
            try { startDate = LocalDate.parse(dateStr.trim()); } catch (Exception ignored) {}
        }

        LocalDate endDate = startDate;
        if (endDateStr != null && !endDateStr.isBlank()) {
            try { endDate = LocalDate.parse(endDateStr.trim()); } catch (Exception ignored) {}
        } else if (startDateStr != null && !startDateStr.isBlank()) {
            endDate = startDate;
        } else if (dateStr != null && !dateStr.isBlank()) {
            try { endDate = LocalDate.parse(dateStr.trim()); } catch (Exception ignored) {}
        }

        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(18, 0);

        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;

        if (startStr != null && startStr.contains("T")) {
            try { startDateTime = LocalDateTime.parse(startStr); } catch (Exception ignored) {}
        } else if (startStr != null && !startStr.isBlank()) {
            try { startTime = LocalTime.parse(startStr.trim()); } catch (Exception ignored) {}
        }

        if (endStr != null && endStr.contains("T")) {
            try { endDateTime = LocalDateTime.parse(endStr); } catch (Exception ignored) {}
        } else if (endStr != null && !endStr.isBlank()) {
            try { endTime = LocalTime.parse(endStr.trim()); } catch (Exception ignored) {}
        }

        if (startDateTime == null) {
            startDateTime = LocalDateTime.of(startDate, startTime);
        }
        if (endDateTime == null) {
            endDateTime = LocalDateTime.of(endDate, endTime);
        }

        if (endDateTime.isBefore(startDateTime) || endDateTime.isEqual(startDateTime)) {
            endDateTime = startDateTime.plusHours(4);
            endDate = endDateTime.toLocalDate();
            endTime = endDateTime.toLocalTime();
        } else {
            startDate = startDateTime.toLocalDate();
            startTime = startDateTime.toLocalTime();
            endDate = endDateTime.toLocalDate();
            endTime = endDateTime.toLocalTime();
        }

        ZoneOffset offset = ZoneOffset.ofHours(7);
        List<VehicleTypeSearchResultDTO> rawResults = slotService.searchAvailableVehicleTypes(
                startDateTime.atOffset(offset), endDateTime.atOffset(offset), category);

        // Filter by keyword, brand, transmission, seats, max price
        List<VehicleTypeSearchResultDTO> results = rawResults.stream()
                .filter(v -> minSeats == null || minSeats == 0 || (v.getSeats() != null && v.getSeats() >= minSeats))
                .filter(v -> maxPrice == null || v.getPricePerHour().compareTo(maxPrice) <= 0)
                .filter(v -> {
                    if (keyword == null || keyword.isBlank()) return true;
                    String kw = keyword.trim().toLowerCase();
                    String name = v.getName() != null ? v.getName().toLowerCase() : "";
                    String b = v.getBrand() != null ? v.getBrand().toLowerCase() : "";
                    String display = v.getDisplayName() != null ? v.getDisplayName().toLowerCase() : "";
                    return name.contains(kw) || b.contains(kw) || display.contains(kw);
                })
                .filter(v -> {
                    if (brand == null || brand.isBlank() || brand.equalsIgnoreCase("all")) return true;
                    return v.getBrand() != null && v.getBrand().equalsIgnoreCase(brand.trim());
                })
                .filter(v -> {
                    if (transmission == null || transmission.isBlank() || transmission.equalsIgnoreCase("all")) return true;
                    return v.getTransmission() != null && v.getTransmission().equalsIgnoreCase(transmission.trim());
                })
                .toList();

        long rentalHours = Math.max(1, Duration.between(startDateTime, endDateTime).toHours());
        long rentalDays = rentalHours / 24;
        long remainingHours = rentalHours % 24;
        String durationDisplay = (rentalDays > 0 ? (rentalDays + "d ") : "") + (remainingHours > 0 ? (remainingHours + "h") : (rentalDays == 0 ? "1h" : ""));

        model.addAttribute("today", LocalDate.now().toString());
        model.addAttribute("selectedStartDate", startDate.toString());
        model.addAttribute("selectedEndDate", endDate.toString());
        model.addAttribute("selectedDate", startDate.toString());
        model.addAttribute("selectedStart", startTime.toString().substring(0, 5));
        model.addAttribute("selectedEnd", endTime.toString().substring(0, 5));
        model.addAttribute("startTime", startDateTime);
        model.addAttribute("endTime", endDateTime);
        model.addAttribute("keyword", keyword != null ? keyword.trim() : "");
        model.addAttribute("brand", brand);
        model.addAttribute("category", category);
        model.addAttribute("minSeats", minSeats);
        model.addAttribute("transmission", transmission);
        model.addAttribute("maxPrice", maxPrice != null ? maxPrice : BigDecimal.valueOf(500000));
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("rentalHours", rentalHours);
        model.addAttribute("rentalDays", rentalDays);
        model.addAttribute("remainingHours", remainingHours);
        model.addAttribute("durationDisplay", durationDisplay);
        model.addAttribute("results", results);
        model.addAttribute("allCount", results.size());
        model.addAttribute("brands", vehicleTypeRepository.findDistinctBrands());
        model.addAttribute("categories", vehicleTypeRepository.findDistinctCategories());
        return "bookings/search";
    }

    // BƯỚC 3: Màn hình Checkout / Xác nhận đơn đặt xe
    @GetMapping("/bookings/checkout")
    public String checkout(
            @RequestParam("vehicleTypeId") Long vehicleTypeId,
            @RequestParam(value = "startDate", required = false) String startDateStr,
            @RequestParam(value = "endDate", required = false) String endDateStr,
            @RequestParam(value = "startTime", required = false) String startStr,
            @RequestParam(value = "endTime", required = false) String endStr,
            @RequestParam(value = "date", required = false) String dateStr,
            HttpSession session, Model model, RedirectAttributes ra) {

        Long customerId = (Long) session.getAttribute("customerId");
        if (customerId == null) {
            ra.addFlashAttribute("error", "Please sign in to proceed with your booking.");
            return "redirect:/login";
        }

        VehicleType vt = vehicleTypeRepository.findById(vehicleTypeId).orElse(null);
        if (vt == null) {
            ra.addFlashAttribute("error", "Vehicle type not found.");
            return "redirect:/search";
        }

        LocalDateTime startTime = null;
        LocalDateTime endTime = null;

        if (startStr != null && startStr.contains("T")) {
            try { startTime = LocalDateTime.parse(startStr); } catch (Exception ignored) {}
        }
        if (endStr != null && endStr.contains("T")) {
            try { endTime = LocalDateTime.parse(endStr); } catch (Exception ignored) {}
        }

        if (startTime == null || endTime == null) {
            LocalDate sDate = (startDateStr != null && !startDateStr.isBlank())
                    ? LocalDate.parse(startDateStr.trim())
                    : ((dateStr != null && !dateStr.isBlank()) ? LocalDate.parse(dateStr.trim()) : LocalDate.now().plusDays(1));
            LocalDate eDate = (endDateStr != null && !endDateStr.isBlank())
                    ? LocalDate.parse(endDateStr.trim())
                    : sDate;
            LocalTime s = (startStr != null && !startStr.isBlank() && !startStr.contains("T")) ? LocalTime.parse(startStr.trim()) : LocalTime.of(9, 0);
            LocalTime e = (endStr != null && !endStr.isBlank() && !endStr.contains("T")) ? LocalTime.parse(endStr.trim()) : LocalTime.of(18, 0);
            startTime = LocalDateTime.of(sDate, s);
            endTime = LocalDateTime.of(eDate, e);
        }

        if (endTime.isBefore(startTime) || endTime.isEqual(startTime)) {
            endTime = startTime.plusHours(4);
        }

        ZoneOffset offset = ZoneOffset.ofHours(7);
        List<Vehicle> availableCars = slotService.findAvailableVehiclesInTimeRange(
                vehicleTypeId, startTime.atOffset(offset), endTime.atOffset(offset));

        if (availableCars.isEmpty()) {
            ra.addFlashAttribute("error", "Sorry, no cars of this type are available for the selected timeframe.");
            return "redirect:/search";
        }

        long rentalHours = Math.max(1, Duration.between(startTime, endTime).toHours());
        long rentalDays = rentalHours / 24;
        long remainingHours = rentalHours % 24;
        String durationDisplay = (rentalDays > 0 ? (rentalDays + "d ") : "") + (remainingHours > 0 ? (remainingHours + "h") : (rentalDays == 0 ? "1h" : ""));
        BigDecimal estimatedTotal = vt.getPricePerHour().multiply(BigDecimal.valueOf(rentalHours));
        BigDecimal depositAmount = estimatedTotal.multiply(BigDecimal.valueOf(0.5)).setScale(0, RoundingMode.HALF_UP);
        BigDecimal remainingAmount = estimatedTotal.subtract(depositAmount);

        BookingCreateDTO form = new BookingCreateDTO();
        form.setVehicleTypeId(vehicleTypeId);
        form.setStartTime(startTime);
        form.setEndTime(endTime);

        LicenseDocument license = licenseDocumentService.getCustomerLicense(customerId);
        boolean isLicenseVerified = licenseDocumentService.isCustomerVerified(customerId);
        String licenseStatus = (license != null) ? license.getStatus() : "not_uploaded";
        session.setAttribute("licenseStatus", licenseStatus);

        model.addAttribute("vehicleType", vt);
        model.addAttribute("bookingForm", form);
        model.addAttribute("availableCars", availableCars);
        model.addAttribute("customerLicense", license);
        model.addAttribute("isLicenseVerified", isLicenseVerified);
        model.addAttribute("licenseStatus", licenseStatus);
        model.addAttribute("rentalHours", rentalHours);
        model.addAttribute("rentalDays", rentalDays);
        model.addAttribute("remainingHours", remainingHours);
        model.addAttribute("durationDisplay", durationDisplay);
        model.addAttribute("estimatedTotal", estimatedTotal);
        model.addAttribute("depositAmount", depositAmount);
        model.addAttribute("remainingAmount", remainingAmount);
        model.addAttribute("vehicleImage", resolveVehicleImage(vt));
        return "bookings/checkout";
    }

    // TẠO ĐƠN: POST /bookings -> Lưu Booking (pending_payment) -> Sang trang cọc
    @PostMapping("/bookings")
    public String createBooking(@Valid @ModelAttribute("bookingForm") BookingCreateDTO form,
                                BindingResult result, HttpSession session, Model model, RedirectAttributes ra) {
        Long customerId = (Long) session.getAttribute("customerId");
        if (customerId == null) return "redirect:/login";

        if (!licenseDocumentService.isCustomerVerified(customerId)) {
            ra.addFlashAttribute("error", "You must have an approved driving license before completing a reservation. Please upload your driving license for verification.");
            return "redirect:/licenses/my-license";
        }

        if (result.hasErrors()) {
            ra.addFlashAttribute("error", "Invalid booking parameters.");
            return "redirect:/search";
        }

        try {
            Booking booking = bookingService.createBooking(customerId, form);
            return "redirect:/bookings/" + booking.getId() + "/pay-deposit";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/bookings/checkout?vehicleTypeId=" + form.getVehicleTypeId() +
                   "&startTime=" + form.getStartTime() + "&endTime=" + form.getEndTime();
        }
    }

    // BƯỚC 4: Thanh toán tiền cọc (Deposit)
    @GetMapping({"/bookings/{id}/pay-deposit", "/bookings/{id}/payment"})
    public String payDepositPage(@PathVariable("id") Long bookingId, HttpSession session, Model model, RedirectAttributes ra) {
        Long customerId = (Long) session.getAttribute("customerId");
        if (customerId == null) return "redirect:/login";

        try {
            BookingDetailDTO detail = bookingService.getBookingDetail(bookingId, customerId);
            model.addAttribute("booking", detail);
            return "bookings/deposit";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/bookings/my-bookings";
        }
    }

    @PostMapping("/bookings/{id}/pay-deposit")
    public String processDeposit(@PathVariable("id") Long bookingId, HttpSession session, RedirectAttributes ra) {
        Long customerId = (Long) session.getAttribute("customerId");
        if (customerId == null) return "redirect:/login";

        try {
            paymentService.processDeposit(bookingId);
            return "redirect:/bookings/" + bookingId + "/success";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Payment failed: " + e.getMessage());
            return "redirect:/bookings/" + bookingId + "/pay-deposit";
        }
    }

    // BƯỚC 5: Trang xác nhận đặt xe thành công
    @GetMapping("/bookings/{id}/success")
    public String bookingSuccess(@PathVariable("id") Long bookingId, HttpSession session, Model model, RedirectAttributes ra) {
        Long customerId = (Long) session.getAttribute("customerId");
        if (customerId == null) return "redirect:/login";

        try {
            BookingDetailDTO detail = bookingService.getBookingDetail(bookingId, customerId);
            model.addAttribute("booking", detail);
            return "bookings/success";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/bookings/my-bookings";
        }
    }

    // Lịch sử đơn của khách
    @GetMapping("/bookings/my-bookings")
    public String myBookings(@RequestParam(value = "status", required = false, defaultValue = "all") String filterStatus,
                             HttpSession session, Model model, RedirectAttributes ra) {
        Long customerId = (Long) session.getAttribute("customerId");
        if (customerId == null) {
            ra.addFlashAttribute("error", "Please sign in to view your bookings.");
            return "redirect:/login";
        }

        List<BookingSummaryDTO> all = bookingService.getMyBookings(customerId);
        List<BookingSummaryDTO> filtered = "all".equalsIgnoreCase(filterStatus)
                ? all
                : all.stream().filter(b -> filterStatus.equalsIgnoreCase(b.getStatus())).toList();

        model.addAttribute("bookings", filtered);
        model.addAttribute("allCount", all.size());
        model.addAttribute("currentFilter", filterStatus);
        return "bookings/my-bookings";
    }

    // Chi tiết đơn
    @GetMapping("/bookings/{id}")
    public String bookingDetail(@PathVariable("id") Long bookingId, HttpSession session, Model model, RedirectAttributes ra) {
        Long customerId = (Long) session.getAttribute("customerId");
        if (customerId == null) return "redirect:/login";

        try {
            BookingDetailDTO detail = bookingService.getBookingDetail(bookingId, customerId);
            model.addAttribute("booking", detail);
            return "bookings/detail";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/bookings/my-bookings";
        }
    }

    // Hủy đơn
    @PostMapping("/bookings/{id}/cancel")
    public String cancelBooking(@PathVariable("id") Long bookingId, HttpSession session, RedirectAttributes ra) {
        Long customerId = (Long) session.getAttribute("customerId");
        if (customerId == null) return "redirect:/login";

        try {
            bookingService.cancelBooking(bookingId, customerId);
            ra.addFlashAttribute("success", "Booking cancelled successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/bookings/" + bookingId;
    }

    private String resolveVehicleImage(VehicleType vt) {
        if (vt == null || vt.getImageUrl() == null || vt.getImageUrl().isBlank()) {
            return "https://images.unsplash.com/photo-1619682817481-e994891cd1f5?auto=format&fit=crop&w=800&q=80";
        }
        return vt.getImageUrl();
    }
}
