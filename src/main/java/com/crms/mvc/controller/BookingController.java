package com.crms.mvc.controller;

import com.crms.mvc.util.CurrentAccount;
import com.crms.mvc.dto.BookingCreateDTO;
import com.crms.mvc.dto.BookingDetailDTO;
import com.crms.mvc.dto.BookingSummaryDTO;
import com.crms.mvc.dto.BookingCancelDTO;
import com.crms.mvc.util.DateTimeUtils;
import com.crms.mvc.dto.VehicleTypeSearchResultDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.entity.VehicleType;
import com.crms.mvc.service.VehicleTypeService;
import com.crms.mvc.service.PricingService;
import com.crms.mvc.dto.PriceQuoteDTO;
import com.crms.mvc.service.BookingService;
import com.crms.mvc.service.PaymentService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import com.crms.mvc.entity.IssueStatus;
import com.crms.mvc.dto.IssueDTO;
import com.crms.mvc.service.IssueService;
import com.crms.mvc.service.HandoverService;
import com.crms.mvc.util.MediaUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import com.crms.mvc.entity.LicenseDocument;
import com.crms.mvc.service.LicenseDocumentService;
import com.crms.mvc.dto.ContractTemplateDTO;
import com.crms.mvc.service.ContractTemplateService;
import com.crms.mvc.dto.ReviewDTO;
import com.crms.mvc.dto.ReviewFormDTO;
import com.crms.mvc.service.ReviewService;
import com.crms.mvc.service.TrafficFineService;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class BookingController {
    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final VehicleTypeService vehicleTypeService;
    private final PricingService pricingService;
    private final LicenseDocumentService licenseDocumentService;
    private final ContractTemplateService contractTemplateService;
    private final IssueService issueService;
    private final HandoverService handoverService;
    private final ReviewService reviewService;
    private final TrafficFineService trafficFineService;

    public BookingController(
            BookingService bookingService,
            PaymentService paymentService,
            VehicleTypeService vehicleTypeService,
            LicenseDocumentService licenseDocumentService,
            ContractTemplateService contractTemplateService,
            PricingService pricingService,
            IssueService issueService,
            HandoverService handoverService,
            ReviewService reviewService,
            TrafficFineService trafficFineService) {
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.vehicleTypeService = vehicleTypeService;
        this.licenseDocumentService = licenseDocumentService;
        this.contractTemplateService = contractTemplateService;
        this.pricingService = pricingService;
        this.issueService = issueService;
        this.handoverService = handoverService;
        this.reviewService = reviewService;
        this.trafficFineService = trafficFineService;
    }

    // STEP 1 & 2: Choose rental schedule & filter available vehicle types
    @GetMapping({ "/search", "/vehicles/available" })
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
            try {
                startDate = LocalDate.parse(startDateStr.trim());
            } catch (Exception ignored) {
            }
        } else if (dateStr != null && !dateStr.isBlank()) {
            try {
                startDate = LocalDate.parse(dateStr.trim());
            } catch (Exception ignored) {
            }
        }

        LocalDate endDate = startDate;
        if (endDateStr != null && !endDateStr.isBlank()) {
            try {
                endDate = LocalDate.parse(endDateStr.trim());
            } catch (Exception ignored) {
            }
        } else if (startDateStr != null && !startDateStr.isBlank()) {
            endDate = startDate;
        } else if (dateStr != null && !dateStr.isBlank()) {
            try {
                endDate = LocalDate.parse(dateStr.trim());
            } catch (Exception ignored) {
            }
        }

        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(18, 0);

        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;

        if (startStr != null && startStr.contains("T")) {
            try {
                startDateTime = LocalDateTime.parse(startStr);
            } catch (Exception ignored) {
            }
        } else if (startStr != null && !startStr.isBlank()) {
            try {
                startTime = LocalTime.parse(startStr.trim());
            } catch (Exception ignored) {
            }
        }

        if (endStr != null && endStr.contains("T")) {
            try {
                endDateTime = LocalDateTime.parse(endStr);
            } catch (Exception ignored) {
            }
        } else if (endStr != null && !endStr.isBlank()) {
            try {
                endTime = LocalTime.parse(endStr.trim());
            } catch (Exception ignored) {
            }
        }

        if (startDateTime == null) {
            startDateTime = LocalDateTime.of(startDate, startTime);
        }
        if (endDateTime == null) {
            endDateTime = LocalDateTime.of(endDate, endTime);
        }

        startDate = startDateTime.toLocalDate();
        startTime = startDateTime.toLocalTime();
        endDate = endDateTime.toLocalDate();
        endTime = endDateTime.toLocalTime();

        // Invalid times are reported back to the customer, never silently moved to another slot.
        String scheduleError = null;
        try {
            bookingService.validateRentalSchedule(startDateTime, endDateTime);
        } catch (IllegalArgumentException e) {
            scheduleError = e.getMessage();
        }

        ZoneOffset offset = ZoneOffset.ofHours(7);
        List<VehicleTypeSearchResultDTO> rawResults = scheduleError != null ? List.of()
                : bookingService.searchAvailableVehicleTypes(
                        startDateTime.atOffset(offset), endDateTime.atOffset(offset), category);

        // Filter by keyword, brand, transmission, seats, max price
        List<VehicleTypeSearchResultDTO> results = rawResults.stream()
                .filter(v -> minSeats == null || minSeats == 0 || (v.getSeats() != null && v.getSeats() >= minSeats))
                .filter(v -> maxPrice == null || v.getPricePerHour().compareTo(maxPrice) <= 0)
                .filter(v -> {
                    if (keyword == null || keyword.isBlank())
                        return true;
                    String kw = keyword.trim().toLowerCase();
                    String name = v.getName() != null ? v.getName().toLowerCase() : "";
                    String b = v.getBrand() != null ? v.getBrand().toLowerCase() : "";
                    String display = v.getDisplayName() != null ? v.getDisplayName().toLowerCase() : "";
                    return name.contains(kw) || b.contains(kw) || display.contains(kw);
                })
                .filter(v -> {
                    if (brand == null || brand.isBlank() || brand.equalsIgnoreCase("all"))
                        return true;
                    return v.getBrand() != null && v.getBrand().equalsIgnoreCase(brand.trim());
                })
                .filter(v -> {
                    if (transmission == null || transmission.isBlank() || transmission.equalsIgnoreCase("all"))
                        return true;
                    return v.getTransmission() != null && v.getTransmission().equalsIgnoreCase(transmission.trim());
                })
                .toList();

        long rentalHours = Math.max(1, Duration.between(startDateTime, endDateTime).toHours());
        long rentalDays = rentalHours / 24;
        long remainingHours = rentalHours % 24;
        String durationDisplay = (rentalDays > 0 ? (rentalDays + "d ") : "")
                + (remainingHours > 0 ? (remainingHours + "h") : (rentalDays == 0 ? "1h" : ""));

        model.addAttribute("scheduleError", scheduleError);
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
        model.addAttribute("ratings", reviewService.ratingsByVehicleType());
        model.addAttribute("allCount", results.size());
        model.addAttribute("brands", vehicleTypeService.getDistinctBrands());
        model.addAttribute("categories", vehicleTypeService.getDistinctCategories());
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
            @RequestParam(value = "vehicleId", required = false) Long vehicleId,
            HttpSession session, Model model, RedirectAttributes ra) {

        Long customerId = CurrentAccount.customerId(session).orElse(null);
        if (customerId == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập để tiếp tục đặt xe.");
            return "redirect:/login";
        }

        VehicleType vt = vehicleTypeService.findById(vehicleTypeId).orElse(null);
        if (vt == null) {
            ra.addFlashAttribute("error", "Không tìm thấy dòng xe.");
            return "redirect:/search";
        }

        LocalDateTime startTime = null;
        LocalDateTime endTime = null;

        if (startStr != null && startStr.contains("T")) {
            try {
                startTime = LocalDateTime.parse(startStr);
            } catch (Exception ignored) {
            }
        }
        if (endStr != null && endStr.contains("T")) {
            try {
                endTime = LocalDateTime.parse(endStr);
            } catch (Exception ignored) {
            }
        }

        if (startTime == null || endTime == null) {
            LocalDate sDate = (startDateStr != null && !startDateStr.isBlank())
                    ? LocalDate.parse(startDateStr.trim())
                    : ((dateStr != null && !dateStr.isBlank()) ? LocalDate.parse(dateStr.trim())
                            : LocalDate.now().plusDays(1));
            LocalDate eDate = (endDateStr != null && !endDateStr.isBlank())
                    ? LocalDate.parse(endDateStr.trim())
                    : sDate;
            LocalTime s = (startStr != null && !startStr.isBlank() && !startStr.contains("T"))
                    ? LocalTime.parse(startStr.trim())
                    : LocalTime.of(9, 0);
            LocalTime e = (endStr != null && !endStr.isBlank() && !endStr.contains("T"))
                    ? LocalTime.parse(endStr.trim())
                    : LocalTime.of(18, 0);
            startTime = LocalDateTime.of(sDate, s);
            endTime = LocalDateTime.of(eDate, e);
        }

        try {
            bookingService.validateRentalSchedule(startTime, endTime);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/search";
        }

        ZoneOffset offset = ZoneOffset.ofHours(7);
        List<Vehicle> availableCars = bookingService.findAvailableVehiclesInTimeRange(
                vehicleTypeId, startTime.atOffset(offset), endTime.atOffset(offset));

        if (availableCars.isEmpty()) {
            ra.addFlashAttribute("error", "Sorry, no cars of this type are available for the selected timeframe.");
            return "redirect:/search";
        }

        ContractTemplateDTO contractTerms = contractTemplateService.getActiveTemplate();
        PriceQuoteDTO quote = pricingService.calculatePrice(startTime, endTime, vt.getPricePerHour(),
                contractTerms.getDepositRate(), BigDecimal.ZERO);
        long rentalHours = quote.getRentalHours();
        long rentalDays = rentalHours / 24;
        long remainingHours = rentalHours % 24;
        String durationDisplay = (rentalDays > 0 ? (rentalDays + "d ") : "")
                + (remainingHours > 0 ? (remainingHours + "h") : (rentalDays == 0 ? "1h" : ""));
        BigDecimal estimatedTotal = quote.getTotalPrice();
        BigDecimal depositAmount = quote.getDepositAmount();
        BigDecimal remainingAmount = quote.getRemainingAmount();

        BookingCreateDTO form = new BookingCreateDTO();
        form.setVehicleTypeId(vehicleTypeId);
        form.setStartTime(startTime);
        form.setEndTime(endTime);
        form.setContractTemplateId(contractTerms.getId());
        // Coming from a car's schedule page: pre-select that plate if it is free for these hours.
        if (vehicleId != null) {
            if (availableCars.stream().anyMatch(car -> car.getId().equals(vehicleId))) {
                form.setVehicleId(vehicleId);
            } else {
                model.addAttribute("error", "Xe bạn chọn không trống trong khung giờ này. Hãy chọn xe khác bên dưới hoặc đổi giờ thuê.");
            }
        }

        LicenseDocument license = licenseDocumentService.getCustomerLicense(customerId);
        boolean hasValidLicense = licenseDocumentService.hasValidLicense(customerId, endTime.toLocalDate());

        model.addAttribute("vehicleType", vt);
        model.addAttribute("bookingForm", form);
        model.addAttribute("availableCars", availableCars);
        model.addAttribute("customerLicense", license);
        model.addAttribute("hasValidLicense", hasValidLicense);
        model.addAttribute("licenseExpired", license != null && !hasValidLicense);
        model.addAttribute("rentalHours", rentalHours);
        model.addAttribute("rentalDays", rentalDays);
        model.addAttribute("remainingHours", remainingHours);
        model.addAttribute("durationDisplay", durationDisplay);
        model.addAttribute("estimatedTotal", estimatedTotal);
        model.addAttribute("depositAmount", depositAmount);
        model.addAttribute("remainingAmount", remainingAmount);
        model.addAttribute("vehicleImage", resolveVehicleImage(vt));
        model.addAttribute("contractTerms", contractTerms);
        return "bookings/checkout";
    }

    // TẠO ĐƠN: POST /bookings -> Lưu Booking (pending_payment) -> Sang trang cọc
    @PostMapping("/bookings")
    public String createBooking(@Valid @ModelAttribute("bookingForm") BookingCreateDTO form,
                                BindingResult result, HttpServletRequest request,
                                HttpSession session, RedirectAttributes ra) {
        Long customerId = CurrentAccount.customerId(session).orElse(null);
        if (customerId == null) return "redirect:/login";

        if (result.hasErrors()) {
            ra.addFlashAttribute("error", result.getAllErrors().get(0).getDefaultMessage());
            boolean canReturnToCheckout = form.getVehicleTypeId() != null
                    && form.getStartTime() != null && form.getEndTime() != null;
            return canReturnToCheckout ? redirectToCheckout(form) : "redirect:/search";
        }

        try {
            // Server-side address, not a client header, because it is stored as acceptance
            // evidence.
            Booking booking = bookingService.createBooking(customerId, form, request.getRemoteAddr());
            return "redirect:/bookings/" + booking.getId() + "/pay-deposit";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return redirectToCheckout(form);
        }
    }

    private String redirectToCheckout(BookingCreateDTO form) {
        return "redirect:/bookings/checkout?vehicleTypeId=" + form.getVehicleTypeId()
                + "&startTime=" + form.getStartTime() + "&endTime=" + form.getEndTime();
    }

    // BƯỚC 4: Thanh toán tiền cọc (Deposit)
    @GetMapping({"/bookings/{id}/pay-deposit", "/bookings/{id}/payment"})
    public String payDepositPage(@PathVariable("id") Long bookingId, HttpSession session, Model model, RedirectAttributes ra) {
        Long customerId = CurrentAccount.customerId(session).orElse(null);
        if (customerId == null) return "redirect:/login";

        try {
            BookingDetailDTO detail = bookingService.getBookingDetail(bookingId, customerId);
            if (!"pending_payment".equalsIgnoreCase(detail.getStatus())) {
                if ("confirmed".equalsIgnoreCase(detail.getStatus())) {
                    return "redirect:/bookings/" + bookingId + "/success";
                }
                ra.addFlashAttribute("error",
                        "Đơn đặt xe không ở trạng thái chờ đặt cọc (Trạng thái hiện tại: " + detail.getStatus() + ").");
                return "redirect:/bookings/" + bookingId;
            }
            model.addAttribute("booking", detail);
            return "bookings/deposit";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/bookings/my-bookings";
        }
    }

    // BƯỚC 5: Trang xác nhận đặt xe thành công
    @GetMapping("/bookings/{id}/success")
    public String bookingSuccess(@PathVariable("id") Long bookingId, HttpSession session, Model model, RedirectAttributes ra) {
        Long customerId = CurrentAccount.customerId(session).orElse(null);
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
        Long customerId = CurrentAccount.customerId(session).orElse(null);
        if (customerId == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập để xem chuyến đi của bạn.");
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
        Long customerId = CurrentAccount.customerId(session).orElse(null);
        if (customerId == null) return "redirect:/login";

        try {
            BookingDetailDTO detail = bookingService.getBookingDetail(bookingId, customerId);
            model.addAttribute("booking", detail);

            // Same rule as BookingService.cancelBooking: the terms the customer accepted, never the ACTIVE version.
            ContractTemplateDTO acceptedTerms = detail.getContract() != null ? detail.getContract().getTerms() : null;
            OffsetDateTime cancelDeadline = acceptedTerms != null && detail.getPickupTime() != null
                    ? acceptedTerms.freeCancellationUntil(detail.getPickupTime(), detail.getDepositPaidAt())
                    : null;
            boolean refundEligible = "confirmed".equalsIgnoreCase(detail.getStatus()) && cancelDeadline != null
                    && !OffsetDateTime.now().isAfter(cancelDeadline);

            model.addAttribute("refundEligible", refundEligible);
            model.addAttribute("freeCancellationHours", acceptedTerms != null ? acceptedTerms.getFreeCancellationHours() : null);
            model.addAttribute("cancelDeadline", cancelDeadline);
            model.addAttribute("cancelForm", new BookingCancelDTO());

            List<IssueDTO> issues = issueService.findByBookingForCustomer(bookingId, customerId);
            model.addAttribute("issues", issues);
            model.addAttribute("trafficFines", trafficFineService.findByBookingForCustomer(bookingId, customerId));

            boolean canPay = Booking.STATUS_ACTIVE.equalsIgnoreCase(detail.getStatus());
            List<IssueDTO> payableIssues = issues.stream()
                    .filter(i -> canPay && i.getStatus() == IssueStatus.OPEN && !i.isFeePaid()
                            && i.getFeeAmount() != null && i.getFeeAmount().compareTo(BigDecimal.ZERO) > 0)
                    .toList();
            BigDecimal payableTotal = payableIssues.stream()
                    .map(IssueDTO::getFeeAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            model.addAttribute("payableIssues", payableIssues);
            model.addAttribute("payableTotal", payableTotal);
            model.addAttribute("returnReceipt", handoverService.getReturnReceipt(bookingId));

            if ("completed".equalsIgnoreCase(detail.getStatus())) {
                boolean reviewEligible = reviewService.isEligibleForCustomer(bookingId, customerId);
                model.addAttribute("reviewEligible", reviewEligible);
                if (reviewEligible) {
                    ReviewDTO review = reviewService.findForCustomer(bookingId, customerId);
                    model.addAttribute("review", review);
                    model.addAttribute("reviewForm", new ReviewFormDTO());
                }
            }
            return "bookings/detail";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/bookings/my-bookings";
        }
    }

    @PostMapping("/bookings/{id}/review")
    public String submitReview(@PathVariable("id") Long bookingId,
                               @Valid @ModelAttribute("reviewForm") ReviewFormDTO form,
                               BindingResult result, HttpSession session, RedirectAttributes ra) {
        Long customerId = CurrentAccount.customerId(session).orElse(null);
        if (customerId == null) return "redirect:/login";

        if (result.hasErrors()) {
            ra.addFlashAttribute("error", result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/bookings/" + bookingId;
        }

        try {
            reviewService.submit(bookingId, customerId, form);
            ra.addFlashAttribute("success", "Cảm ơn bạn đã chia sẻ đánh giá về chuyến đi!");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/bookings/" + bookingId;
    }

    // Hủy đơn
    @PostMapping("/bookings/{id}/cancel")
    public String cancelBooking(@PathVariable("id") Long bookingId,
            @ModelAttribute("cancelForm") BookingCancelDTO form,
            HttpSession session, RedirectAttributes ra) {
        Long customerId = CurrentAccount.customerId(session).orElse(null);
        if (customerId == null)
            return "redirect:/login";

        try {
            BookingDetailDTO detail = bookingService.getBookingDetail(bookingId, customerId);
            boolean wasConfirmed = "confirmed".equalsIgnoreCase(detail.getStatus());
            boolean refundCreated = bookingService.cancelBooking(bookingId, customerId, form);

            if (refundCreated) {
                String formattedDeposit = detail.getDepositAmount() != null
                        ? String.format("%,d", detail.getDepositAmount().longValue())
                        : "0";
                ra.addFlashAttribute("success", "Hủy đơn đặt xe thành công! Yêu cầu hoàn cọc " + formattedDeposit
                        + " đ đang được xử lý, tiền sẽ được hoàn vào tài khoản của bạn trong vòng 24h làm việc.");
            } else if (wasConfirmed) {
                ra.addFlashAttribute("warning",
                        "Hủy đơn đặt xe thành công. Do bạn hủy trong vòng 72 giờ trước giờ nhận xe, tiền cọc không được hoàn lại theo điều khoản hợp đồng.");
            } else {
                ra.addFlashAttribute("success", "Đã hủy đơn đặt xe thành công.");
            }
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

    @GetMapping("/bookings/{bookingId}/issues/{issueId}/evidences/{evidenceId}")
    public ResponseEntity<Resource> getIssueEvidence(
            @PathVariable("bookingId") Long bookingId,
            @PathVariable("issueId") Long issueId,
            @PathVariable("evidenceId") Long evidenceId,
            HttpSession session) {
        Long customerId = CurrentAccount.customerId(session).orElse(null);
        if (customerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            Resource file = issueService.loadEvidenceForCustomer(issueId, evidenceId, customerId);
            MediaType mediaType = MediaUtils.resolveMediaType(file.getFilename());
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .body(file);
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}