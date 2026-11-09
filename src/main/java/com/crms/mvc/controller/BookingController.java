package com.crms.mvc.controller;

import com.crms.mvc.util.CurrentAccount;
import com.crms.mvc.dto.BookingCreateDTO;
import com.crms.mvc.dto.BookingQuoteDTO;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import com.crms.mvc.dto.BookingDetailDTO;
import com.crms.mvc.dto.BookingSummaryDTO;
import com.crms.mvc.dto.BookingCancelDTO;
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
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
import java.util.NoSuchElementException;
import com.crms.mvc.service.BookingQueryService;
import com.crms.mvc.service.VehicleAvailabilityService;
import com.crms.mvc.dto.RentalWindow;
import com.crms.mvc.dto.RentalDuration;
import com.crms.mvc.util.RentalWindowParser;
import com.crms.mvc.util.VehicleImages;

@Controller
public class BookingController {
    private final BookingService bookingService;
    private final BookingQueryService bookingQueryService;
    private final VehicleAvailabilityService vehicleAvailabilityService;
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
            BookingQueryService bookingQueryService,
            VehicleAvailabilityService vehicleAvailabilityService,
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
        this.bookingQueryService = bookingQueryService;
        this.vehicleAvailabilityService = vehicleAvailabilityService;
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
    public String searchAvailableVehicles(@ModelAttribute RentalSearchParams params, Model model) {
        RentalWindow window = params.window();

        // Invalid times are reported back to the customer, never silently moved to another slot.
        String scheduleError = scheduleErrorOf(window);
        List<VehicleTypeSearchResultDTO> results = scheduleError != null ? List.of()
                : params.criteria().narrow(vehicleAvailabilityService.searchAvailableVehicleTypes(
                        window.startAt(), window.endAt(), params.getCategory()));
        RentalDuration duration = window.duration();

        model.addAttribute("scheduleError", scheduleError);
        model.addAttribute("today", LocalDate.now().toString());
        model.addAttribute("selectedStartDate", window.startDate().toString());
        model.addAttribute("selectedEndDate", window.endDate().toString());
        model.addAttribute("selectedDate", window.startDate().toString());
        model.addAttribute("selectedStart", window.startTime().toString().substring(0, 5));
        model.addAttribute("selectedEnd", window.endTime().toString().substring(0, 5));
        model.addAttribute("startTime", window.start());
        model.addAttribute("endTime", window.end());
        model.addAttribute("keyword", params.getKeyword() != null ? params.getKeyword().trim() : "");
        model.addAttribute("brand", params.getBrand());
        model.addAttribute("category", params.getCategory());
        model.addAttribute("minSeats", params.getSeats());
        model.addAttribute("transmission", params.getTransmission());
        model.addAttribute("maxPrice", params.getMaxPrice() != null ? params.getMaxPrice() : BigDecimal.valueOf(500000));
        model.addAttribute("sortBy", params.getSortBy());
        addDuration(model, duration);
        model.addAttribute("results", results);
        model.addAttribute("ratings", reviewService.ratingsByVehicleType());
        model.addAttribute("allCount", results.size());
        model.addAttribute("brands", vehicleTypeService.getDistinctBrands());
        model.addAttribute("categories", vehicleTypeService.getDistinctCategories());
        return "screens/customer/bookings/search/index";
    }

    /** Why the window cannot be rented, or null when it can. */
    private String scheduleErrorOf(RentalWindow window) {
        try {
            bookingService.validateRentalSchedule(window.start(), window.end());
            return null;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    private static void addDuration(Model model, RentalDuration duration) {
        model.addAttribute("rentalHours", duration.hours());
        model.addAttribute("rentalDays", duration.days());
        model.addAttribute("remainingHours", duration.remainingHours());
        model.addAttribute("durationDisplay", duration.display());
    }

    @GetMapping(value = "/bookings/quote", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<BookingQuoteDTO> previewQuote(
            @RequestParam Long vehicleTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam Long contractTemplateId,
            @RequestParam(required = false) String promoCode,
            HttpSession session) {
        if (CurrentAccount.customerId(session).isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BookingQuoteDTO.invalid("Vui lòng đăng nhập bằng tài khoản khách hàng."));
        }
        BookingCreateDTO request = new BookingCreateDTO();
        request.setVehicleTypeId(vehicleTypeId);
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        request.setContractTemplateId(contractTemplateId);
        request.setPromotionCode(promoCode);
        try {
            return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                    .body(BookingQuoteDTO.from(bookingService.previewPrice(request)));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(BookingQuoteDTO.invalid(ex.getMessage()));
        }
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

        RentalWindow window = RentalWindowParser.parse(startDateStr, endDateStr, dateStr, startStr, endStr);
        String scheduleError = scheduleErrorOf(window);
        if (scheduleError != null) {
            ra.addFlashAttribute("error", scheduleError);
            return "redirect:/search";
        }

        List<Vehicle> availableCars = vehicleAvailabilityService.findAvailableVehiclesInTimeRange(
                vehicleTypeId, window.startAt(), window.endAt());
        if (availableCars.isEmpty()) {
            ra.addFlashAttribute("error", "Sorry, no cars of this type are available for the selected timeframe.");
            return "redirect:/search";
        }

        ContractTemplateDTO contractTerms = contractTemplateService.getActiveTemplate();
        PriceQuoteDTO quote = pricingService.calculatePrice(window.start(), window.end(), vt.getPricePerHour(),
                contractTerms.getDepositRate(), BigDecimal.ZERO);

        BookingCreateDTO form = new BookingCreateDTO();
        form.setVehicleTypeId(vehicleTypeId);
        form.setStartTime(window.start());
        form.setEndTime(window.end());
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
        boolean hasValidLicense = licenseDocumentService.hasValidLicense(customerId, window.endDate());

        model.addAttribute("vehicleType", vt);
        model.addAttribute("bookingForm", form);
        model.addAttribute("availableCars", availableCars);
        model.addAttribute("customerLicense", license);
        model.addAttribute("hasValidLicense", hasValidLicense);
        model.addAttribute("licenseExpired", license != null && !hasValidLicense);
        addDuration(model, new RentalDuration(quote.getRentalHours()));
        model.addAttribute("estimatedTotal", quote.getTotalPrice());
        model.addAttribute("depositAmount", quote.getDepositAmount());
        model.addAttribute("remainingAmount", quote.getRemainingAmount());
        model.addAttribute("vehicleImage", VehicleImages.urlOf(vt));
        model.addAttribute("contractTerms", contractTerms);
        return "screens/customer/bookings/checkout/index";
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
            BookingDetailDTO detail = bookingQueryService.getBookingDetail(bookingId, customerId);
            if (!Booking.STATUS_PENDING_PAYMENT.equalsIgnoreCase(detail.getStatus())) {
                if (Booking.STATUS_CONFIRMED.equalsIgnoreCase(detail.getStatus())) {
                    return "redirect:/bookings/" + bookingId + "/success";
                }
                ra.addFlashAttribute("error",
                        "Đơn đặt xe không ở trạng thái chờ đặt cọc (Trạng thái hiện tại: " + detail.getStatus() + ").");
                return "redirect:/bookings/" + bookingId;
            }
            model.addAttribute("booking", detail);
            return "screens/customer/bookings/deposit/index";
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
            BookingDetailDTO detail = bookingQueryService.getBookingDetail(bookingId, customerId);
            model.addAttribute("booking", detail);
            return "screens/customer/bookings/success/index";
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

        List<BookingSummaryDTO> all = bookingQueryService.getMyBookings(customerId);
        List<BookingSummaryDTO> filtered = "all".equalsIgnoreCase(filterStatus)
                ? all
                : all.stream().filter(b -> filterStatus.equalsIgnoreCase(b.getStatus())).toList();

        model.addAttribute("bookings", filtered);
        model.addAttribute("allCount", all.size());
        model.addAttribute("currentFilter", filterStatus);
        return "screens/customer/bookings/my-bookings/index";
    }

    // Chi tiết đơn
    @GetMapping("/bookings/{id}")
    public String bookingDetail(@PathVariable("id") Long bookingId, HttpSession session, Model model, RedirectAttributes ra) {
        Long customerId = CurrentAccount.customerId(session).orElse(null);
        if (customerId == null) return "redirect:/login";

        try {
            BookingDetailDTO detail = bookingQueryService.getBookingDetail(bookingId, customerId);
            model.addAttribute("booking", detail);

            // Same rule as BookingService.cancelBooking: the terms the customer accepted, never the ACTIVE version.
            ContractTemplateDTO acceptedTerms = detail.getContract() != null ? detail.getContract().getTerms() : null;
            OffsetDateTime cancelDeadline = acceptedTerms != null && detail.getPickupTime() != null
                    ? acceptedTerms.freeCancellationUntil(detail.getPickupTime(), detail.getDepositPaidAt())
                    : null;
            boolean refundEligible = Booking.STATUS_CONFIRMED.equalsIgnoreCase(detail.getStatus()) && cancelDeadline != null
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

            if (Booking.STATUS_COMPLETED.equalsIgnoreCase(detail.getStatus())) {
                boolean reviewEligible = reviewService.isEligibleForCustomer(bookingId, customerId);
                model.addAttribute("reviewEligible", reviewEligible);
                if (reviewEligible) {
                    ReviewDTO review = reviewService.findForCustomer(bookingId, customerId);
                    model.addAttribute("review", review);
                    model.addAttribute("reviewForm", new ReviewFormDTO());
                }
            }
            return "screens/customer/bookings/detail/index";
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
            BookingDetailDTO detail = bookingQueryService.getBookingDetail(bookingId, customerId);
            boolean wasConfirmed = Booking.STATUS_CONFIRMED.equalsIgnoreCase(detail.getStatus());
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
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}