package com.crms.mvc.controller;

import com.crms.mvc.enums.PaymentMethod;
import com.crms.mvc.enums.FuelType;
import com.crms.mvc.enums.Transmission;
import com.crms.mvc.enums.VehicleCategory;
import com.crms.mvc.support.TestSessions;
import org.springframework.context.annotation.Import;
import com.crms.mvc.support.PassThroughSessionAccountService;
import com.crms.mvc.dto.BookingDetailDTO;
import com.crms.mvc.dto.ContractDTO;
import com.crms.mvc.dto.ContractTemplateDTO;
import com.crms.mvc.dto.ReviewDTO;
import com.crms.mvc.enums.ContractTemplateStatus;
import com.crms.mvc.entity.LicensePlate;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.entity.VehicleType;
import com.crms.mvc.service.VehicleTypeService;
import com.crms.mvc.service.PricingService;
import com.crms.mvc.service.impl.PricingServiceImpl;
import com.crms.mvc.service.BookingService;
import com.crms.mvc.service.ContractTemplateService;
import com.crms.mvc.service.LicenseDocumentService;
import com.crms.mvc.service.PaymentService;
import com.crms.mvc.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(PassThroughSessionAccountService.class)
@WebMvcTest(BookingController.class)
class BookingContractFlowTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private BookingService bookingService;
    @MockitoBean private com.crms.mvc.service.BookingQueryService bookingQueryService;
    @MockitoBean private com.crms.mvc.service.VehicleAvailabilityService vehicleAvailabilityService;
    @MockitoBean private PaymentService paymentService;
    @MockitoBean private VehicleTypeService vehicleTypeService;
    @MockitoBean private PricingService pricingService;
    @MockitoBean private LicenseDocumentService licenseDocumentService;
    @MockitoBean private ContractTemplateService contractTemplateService;
    @MockitoBean private com.crms.mvc.service.IssueService issueService;
    @MockitoBean private com.crms.mvc.service.HandoverService handoverService;
    @MockitoBean private ReviewService reviewService;
    @MockitoBean private com.crms.mvc.service.TrafficFineService trafficFineService;

    private MockHttpSession customerSession;
    private ContractTemplateDTO terms;

    @BeforeEach
    void setUp() {
        customerSession = TestSessions.customer(1L, "Nguyễn Văn An");

        terms = new ContractTemplateDTO();
        terms.setId(3L);
        terms.setVersionNo(3);
        terms.setTitle("Hợp đồng thuê xe ô tô tự lái");
        terms.setTermsContent("1. Không hút thuốc trong xe.");
        terms.setDepositRate(new BigDecimal("0.3000"));
        terms.setLateFeeMultiplier(new BigDecimal("1.50"));
        terms.setFreeCancellationHours(48);
        terms.setStatus(ContractTemplateStatus.ACTIVE);
        when(contractTemplateService.getActiveTemplate()).thenReturn(terms);
        when(pricingService.calculatePrice(any(), any(), any(), any(), any())).thenAnswer(inv -> new PricingServiceImpl().calculatePrice(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2), inv.getArgument(3), inv.getArgument(4)));
    }

    @Test
    void checkoutShowsActiveTermsVersionAndDepositFromIt() throws Exception {
        VehicleType type = new VehicleType();
        type.setId(1L);
        type.setName("Vios");
        type.setBrand("Toyota");
        type.setCategory(VehicleCategory.SEDAN);
        type.setSeats(5);
        type.setTransmission(Transmission.AUTOMATIC);
        type.setFuelType(FuelType.GASOLINE);
        type.setPricePerHour(new BigDecimal("100000"));
        LicensePlate plate = new LicensePlate();
        plate.setPlate("51A-123.45");
        Vehicle car = new Vehicle();
        car.setId(9L);
        car.setColor("White");
        car.setCurrentPlate(plate);
        car.setVehicleType(type);
        when(vehicleTypeService.findById(1L)).thenReturn(Optional.of(type));
        when(vehicleAvailabilityService.findAvailableVehiclesInTimeRange(eq(1L), any(), any())).thenReturn(List.of(car));

        String html = mvc.perform(get("/bookings/checkout").session(customerSession)
                        .param("vehicleTypeId", "1")
                        .param("startTime", "2026-10-24T09:00")
                        .param("endTime", "2026-10-24T19:00"))
                .andExpect(status().isOk())
                .andExpect(view().name("screens/customer/bookings/checkout/index"))
                // 10 hours x 100,000 = 1,000,000; deposit 30% from version 3
                .andExpect(content().string(containsString("300,000")))
                .andExpect(content().string(containsString("name=\"contractTemplateId\" value=\"3\"")))
                .andExpect(content().string(containsString("DriveNow Mobility Vietnam")))
                .andExpect(content().string(containsString("Không hút thuốc trong xe")))
                .andReturn().getResponse().getContentAsString();

        // Clickwrap checkbox must never be pre-ticked
        int start = html.indexOf("id=\"termsCheckbox\"");
        String checkboxTag = html.substring(start, html.indexOf("/>", start));
        assertTrue(checkboxTag.contains("name=\"acceptedTerms\""), checkboxTag);
        assertFalse(checkboxTag.contains("checked"), checkboxTag);
    }

    @Test
    void bookingIsRejectedWhenTermsAreNotAccepted() throws Exception {
        mvc.perform(post("/bookings").session(customerSession)
                        .param("vehicleTypeId", "1")
                        .param("startTime", "2026-10-24T09:00")
                        .param("endTime", "2026-10-24T19:00")
                        .param("contractTemplateId", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/bookings/checkout?vehicleTypeId=1*"))
                .andExpect(flash().attribute("error", "Bạn cần đồng ý với điều khoản thuê xe để tiếp tục"));

        verify(bookingService, never()).createBooking(anyLong(), any(), any());
    }

    @Test
    void acceptedBookingStoresClientIpAsEvidence() throws Exception {
        com.crms.mvc.entity.Booking booking = new com.crms.mvc.entity.Booking();
        booking.setId(42L);
        when(bookingService.createBooking(eq(1L), any(), eq("203.0.113.7"))).thenReturn(booking);

        mvc.perform(post("/bookings").session(customerSession)
                        .with(request -> { request.setRemoteAddr("203.0.113.7"); return request; })
                        .param("vehicleTypeId", "1")
                        .param("startTime", "2026-10-24T09:00")
                        .param("endTime", "2026-10-24T19:00")
                        .param("contractTemplateId", "3")
                        .param("acceptedTerms", "true"))
                .andExpect(redirectedUrl("/bookings/42/pay-deposit"));
    }

    @Test
    void bookingDoesNotRequireADrivingLicense() throws Exception {
        // The customer has no license on file (mock default): the booking must still go through.
        com.crms.mvc.entity.Booking booking = new com.crms.mvc.entity.Booking();
        booking.setId(43L);
        when(bookingService.createBooking(eq(1L), any(), any())).thenReturn(booking);

        mvc.perform(post("/bookings").session(customerSession)
                        .param("vehicleTypeId", "1")
                        .param("startTime", "2026-10-24T09:00")
                        .param("endTime", "2026-10-24T19:00")
                        .param("contractTemplateId", "3")
                        .param("acceptedTerms", "true"))
                .andExpect(redirectedUrl("/bookings/43/pay-deposit"));
    }

    @Test
    void bookingDetailOffersPdfOnceIssued() throws Exception {
        when(bookingQueryService.getBookingDetail(42L, 1L)).thenReturn(bookingWithContract("confirmed", true));

        mvc.perform(get("/bookings/42").session(customerSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/bookings/42/contract")))
                .andExpect(content().string(containsString("CTR-20261023-0042")))
                .andExpect(content().string(containsString("30%")))
                .andExpect(content().string(containsString("150%")));
    }

    @Test
    void completedBookingShowsReviewFormBelowPaymentLedger() throws Exception {
        when(bookingQueryService.getBookingDetail(42L, 1L)).thenReturn(bookingWithContract("completed", true));
        when(reviewService.isEligibleForCustomer(42L, 1L)).thenReturn(true);

        mvc.perform(get("/bookings/42").session(customerSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Đánh giá dịch vụ")))
                .andExpect(content().string(containsString("Gửi đánh giá")))
                .andExpect(content().string(containsString("/bookings/42/review")));

        verify(reviewService).findForCustomer(42L, 1L);
    }

    @Test
    void unfinishedBookingDoesNotShowReviewForm() throws Exception {
        when(bookingQueryService.getBookingDetail(42L, 1L)).thenReturn(bookingWithContract("active", true));

        mvc.perform(get("/bookings/42").session(customerSession))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Đánh giá dịch vụ"))))
                .andExpect(content().string(not(containsString("/bookings/42/review"))));

        verify(reviewService, never()).findForCustomer(anyLong(), anyLong());
        verify(reviewService, never()).isEligibleForCustomer(anyLong(), anyLong());
    }

    @Test
    void completedBookingWithPendingTransactionDoesNotShowReviewForm() throws Exception {
        when(bookingQueryService.getBookingDetail(42L, 1L)).thenReturn(bookingWithContract("completed", true));
        when(reviewService.isEligibleForCustomer(42L, 1L)).thenReturn(false);

        mvc.perform(get("/bookings/42").session(customerSession))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Đánh giá dịch vụ"))))
                .andExpect(content().string(not(containsString("/bookings/42/review"))));

        verify(reviewService, never()).findForCustomer(anyLong(), anyLong());
    }

    @Test
    void completedBookingDisplaysPreviouslySubmittedReview() throws Exception {
        when(bookingQueryService.getBookingDetail(42L, 1L)).thenReturn(bookingWithContract("completed", true));
        when(reviewService.isEligibleForCustomer(42L, 1L)).thenReturn(true);
        ReviewDTO review = new ReviewDTO();
        review.setRating(4);
        review.setComment("Xe sạch và nhân viên hỗ trợ tốt.");
        when(reviewService.findForCustomer(42L, 1L)).thenReturn(review);

        mvc.perform(get("/bookings/42").session(customerSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("★★★★☆")))
                .andExpect(content().string(containsString("Xe sạch và nhân viên hỗ trợ tốt.")))
                .andExpect(content().string(containsString("/bookings/42/review/update")))
                .andExpect(content().string(containsString("Cập nhật đánh giá")))
                .andExpect(content().string(containsString("value=\"4\" id=\"rating4\" name=\"rating\" checked=\"checked\"")))
                .andExpect(content().string(not(containsString("Gửi đánh giá"))));
    }

    @Test
    void customerCanSubmitReviewForCompletedBooking() throws Exception {
        mvc.perform(post("/bookings/42/review").session(customerSession)
                        .param("rating", "5")
                        .param("comment", "Xe sạch, nhân viên hỗ trợ tốt."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings/42"))
                .andExpect(flash().attribute("success", "Cảm ơn bạn đã chia sẻ đánh giá về chuyến đi!"));

        verify(reviewService).submit(eq(42L), eq(1L), any());
    }

    @Test
    void customerCanUpdateReviewForCompletedBooking() throws Exception {
        mvc.perform(post("/bookings/42/review/update").session(customerSession)
                        .param("rating", "4")
                        .param("comment", "Đã cập nhật đánh giá."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings/42"))
                .andExpect(flash().attribute("success", "Đánh giá đã được cập nhật."));

        verify(reviewService).update(eq(42L), eq(1L), any());
    }

    @Test
    void depositPageUsesTermsVersionAcceptedAtCheckout() throws Exception {
        when(bookingQueryService.getBookingDetail(42L, 1L)).thenReturn(bookingWithContract("pending_payment", false));

        mvc.perform(get("/bookings/42/pay-deposit").session(customerSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("30%")))
                .andExpect(content().string(containsString("48")))
                .andExpect(content().string(not(containsString("/bookings/42/contract\""))));
    }

    @Test
    void successPageShowsDepositPercentOfAcceptedTerms() throws Exception {
        when(bookingQueryService.getBookingDetail(42L, 1L)).thenReturn(bookingWithContract("confirmed", true));

        mvc.perform(get("/bookings/42/success").session(customerSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("30%, VNPay")));
    }

    @Test
    void customerSeesTheIssuesChargedOnTheirBooking() throws Exception {
        when(bookingQueryService.getBookingDetail(42L, 1L)).thenReturn(bookingWithContract("completed", true));
        com.crms.mvc.dto.IssueDTO issue = new com.crms.mvc.dto.IssueDTO();
        issue.setId(7L);
        issue.setBookingId(42L);
        issue.setIssueType(com.crms.mvc.enums.IssueType.DAMAGE);
        issue.setStatus(com.crms.mvc.enums.IssueStatus.RESOLVED);
        issue.setSubject("Scratch on the bumper");
        issue.setDescription("About 15 cm.");
        issue.setFeeAmount(new BigDecimal("200000"));
        issue.setOccurredAt(java.time.OffsetDateTime.now().minusDays(1));
        issue.setEvidences(List.of(new com.crms.mvc.dto.IssueDTO.EvidenceDTO(3L, "issue-1.png", java.time.OffsetDateTime.now())));
        when(issueService.findByBookingForCustomer(42L, 1L)).thenReturn(List.of(issue));

        mvc.perform(get("/bookings/42").session(customerSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sự cố trong chuyến thuê")))
                .andExpect(content().string(containsString("Scratch on the bumper")))
                .andExpect(content().string(containsString("200,000")))
                .andExpect(content().string(containsString("/bookings/42/issues/7/evidences/3")));
    }

    @Test
    void customerSeesTheReturnAndSettlementOfTheirBooking() throws Exception {
        when(bookingQueryService.getBookingDetail(42L, 1L)).thenReturn(bookingWithContract("completed", true));
        com.crms.mvc.dto.ReturnReceiptDTO receipt = new com.crms.mvc.dto.ReturnReceiptDTO();
        receipt.setReturnedAt(OffsetDateTime.now());
        receipt.setOdometer(12500);
        receipt.setRentalTotal(new BigDecimal("1500000"));
        receipt.setDepositAmount(new BigDecimal("750000"));
        receipt.setLines(List.of(
                new com.crms.mvc.dto.ReturnReceiptLineDTO("Late return fee", new BigDecimal("300000"), false),
                new com.crms.mvc.dto.ReturnReceiptLineDTO("Scratch", new BigDecimal("400000"), true)));
        receipt.setSettled(true);
        receipt.setAmountDue(new BigDecimal("1050000"));
        receipt.setPaymentMethod("BANK_TRANSFER");
        receipt.setPaidAt(OffsetDateTime.now());
        when(handoverService.getReturnReceipt(42L)).thenReturn(receipt);

        mvc.perform(get("/bookings/42").session(customerSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Trả xe &amp; quyết toán")))
                .andExpect(content().string(containsString("12500 km")))
                .andExpect(content().string(containsString("Late return fee")))
                .andExpect(content().string(containsString("Đã thanh toán trước")))
                .andExpect(content().string(containsString("1,050,000")))
                .andExpect(content().string(containsString("Chuyển khoản")));
    }

    @Test
    void noReturnSectionBeforeTheVehicleIsReturned() throws Exception {
        when(bookingQueryService.getBookingDetail(42L, 1L)).thenReturn(bookingWithContract("active", true));
        when(handoverService.getReturnReceipt(42L)).thenReturn(null);

        mvc.perform(get("/bookings/42").session(customerSession))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Trả xe &amp; quyết toán"))));
    }

    @Test
    void aBookingWithoutIssuesHasNoIssueSection() throws Exception {
        when(bookingQueryService.getBookingDetail(42L, 1L)).thenReturn(bookingWithContract("confirmed", true));
        when(issueService.findByBookingForCustomer(42L, 1L)).thenReturn(List.of());

        mvc.perform(get("/bookings/42").session(customerSession))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Sự cố trong chuyến thuê"))));
    }

    @Test
    void checkoutFromACarSchedulePreselectsThatPlateOnlyWhenItIsFree() throws Exception {
        VehicleType type = new VehicleType();
        type.setId(1L);
        type.setName("Vios");
        type.setBrand("Toyota");
        type.setPricePerHour(new BigDecimal("100000"));
        Vehicle car = new Vehicle();
        car.setId(9L);
        car.setVehicleType(type);
        when(vehicleTypeService.findById(1L)).thenReturn(Optional.of(type));
        when(vehicleAvailabilityService.findAvailableVehiclesInTimeRange(eq(1L), any(), any())).thenReturn(List.of(car));

        String html = mvc.perform(get("/bookings/checkout").session(customerSession)
                        .param("vehicleTypeId", "1").param("vehicleId", "9")
                        .param("startTime", "2026-10-24T09:00").param("endTime", "2026-10-24T19:00"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(html).containsPattern("<input(?=[^>]*name=\"vehicleId\")(?=[^>]*value=\"9\")(?=[^>]*checked=\"checked\")[^>]*>");

        mvc.perform(get("/bookings/checkout").session(customerSession)
                        .param("vehicleTypeId", "1").param("vehicleId", "8")
                        .param("startTime", "2026-10-24T09:00").param("endTime", "2026-10-24T19:00"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Xe bạn chọn không trống trong khung giờ này")));
    }

    @Test
    void anOverdueRentalShowsHowLateItIsAndTheLateFeeSoFar() throws Exception {
        BookingDetailDTO booking = bookingWithContract("active", true);
        booking.setLateReturn(new com.crms.mvc.dto.LateReturnDTO(135, 2, new BigDecimal("400000"),
                OffsetDateTime.of(2026, 11, 7, 17, 0, 0, 0, ZoneOffset.ofHours(7))));
        when(bookingQueryService.getBookingDetail(42L, 1L)).thenReturn(booking);

        mvc.perform(get("/bookings/42").session(customerSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Đã quá giờ trả xe <span>2 giờ 15 phút</span>")))
                .andExpect(content().string(containsString("400,000 đ")))
                .andExpect(content().string(containsString("17:00 07/11/2026")));
    }

    @Test
    void refundEligibilityFollowsTheAcceptedTermsNotTheActiveVersion() throws Exception {
        // Accepted 24h; the ACTIVE version (setUp) says 48h. Pickup is 30h away: free under the accepted terms only.
        ContractTemplateDTO accepted = new ContractTemplateDTO();
        accepted.setDepositRate(new BigDecimal("0.30"));
        accepted.setLateFeeMultiplier(new BigDecimal("1.50"));
        accepted.setFreeCancellationHours(24);
        BookingDetailDTO booking = bookingWithContract("confirmed", true);
        booking.getContract().setTerms(accepted);
        booking.setPickupTime(OffsetDateTime.now().plusHours(30));
        booking.setScheduledEndTime(booking.getPickupTime().plusHours(10));
        when(bookingQueryService.getBookingDetail(42L, 1L)).thenReturn(booking);

        mvc.perform(get("/bookings/42").session(customerSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bạn còn được hủy miễn phí đến")));
    }

    @Test
    void theOwnerCanOpenAPhotoOfTheirIssue() throws Exception {
        when(issueService.loadEvidenceForCustomer(7L, 3L, 1L)).thenReturn(new org.springframework.core.io.ByteArrayResource(new byte[]{1, 2}) {
            @Override
            public String getFilename() {
                return "issue-1.webp";
            }
        });

        mvc.perform(get("/bookings/42/issues/7/evidences/3").session(customerSession))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/webp"));
    }

    @Test
    void aPhotoOfSomeoneElsesIssueIsNotFound() throws Exception {
        when(issueService.loadEvidenceForCustomer(7L, 3L, 1L)).thenThrow(new java.util.NoSuchElementException("Issue not found: 7"));

        mvc.perform(get("/bookings/42/issues/7/evidences/3").session(customerSession))
                .andExpect(status().isNotFound());
    }

    @Test
    void aBookingOffersToPayOpenUnpaidIssueFees() throws Exception {
        when(bookingQueryService.getBookingDetail(42L, 1L)).thenReturn(bookingWithContract("active", true));
        when(issueService.findByBookingForCustomer(42L, 1L)).thenReturn(List.of(
                issueWith(7L, com.crms.mvc.enums.IssueStatus.OPEN, "200000", false),
                issueWith(8L, com.crms.mvc.enums.IssueStatus.OPEN, "500000", false),
                issueWith(9L, com.crms.mvc.enums.IssueStatus.RESOLVED, "999000", true),
                issueWith(10L, com.crms.mvc.enums.IssueStatus.CANCELLED, "300000", false)));

        mvc.perform(get("/bookings/42").session(customerSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Phí sự cố chưa thanh toán")))
                .andExpect(content().string(containsString("/bookings/42/pay-issue-fees")))
                // only open and unpaid: 200,000 + 500,000; the resolved and cancelled ones are left out
                .andExpect(content().string(containsString("700,000")));
    }

    @Test
    void nothingIsOfferedToPayWhenEverythingIsPaidOrCancelled() throws Exception {
        when(bookingQueryService.getBookingDetail(42L, 1L)).thenReturn(bookingWithContract("completed", true));
        when(issueService.findByBookingForCustomer(42L, 1L)).thenReturn(List.of(
                issueWith(9L, com.crms.mvc.enums.IssueStatus.CANCELLED, "999000", false),
                issueWith(10L, com.crms.mvc.enums.IssueStatus.RESOLVED, "300000", true)));

        mvc.perform(get("/bookings/42").session(customerSession))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("/bookings/42/pay-issue-fees"))));
    }

    @Test
    void feesArePayableOnlineDuringActiveRental() throws Exception {
        when(bookingQueryService.getBookingDetail(42L, 1L)).thenReturn(bookingWithContract("active", true));
        when(issueService.findByBookingForCustomer(42L, 1L)).thenReturn(List.of(
                issueWith(7L, com.crms.mvc.enums.IssueStatus.OPEN, "200000", false)));

        mvc.perform(get("/bookings/42").session(customerSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/bookings/42/pay-issue-fees")));
    }

    @Test
    void aPaymentLeftPendingCanBeRetried() throws Exception {
        when(bookingQueryService.getBookingDetail(42L, 1L)).thenReturn(bookingWithContract("active", true));
        com.crms.mvc.dto.IssueDTO pending = issueWith(7L, com.crms.mvc.enums.IssueStatus.OPEN, "200000", false);
        pending.setFeeLocked(true); // a payment has taken it but has not been paid yet
        when(issueService.findByBookingForCustomer(42L, 1L)).thenReturn(List.of(pending));

        mvc.perform(get("/bookings/42").session(customerSession))
                .andExpect(content().string(containsString("/bookings/42/pay-issue-fees")));
    }

    private com.crms.mvc.dto.IssueDTO issueWith(Long id, com.crms.mvc.enums.IssueStatus status, String fee, boolean paid) {
        com.crms.mvc.dto.IssueDTO issue = new com.crms.mvc.dto.IssueDTO();
        issue.setId(id);
        issue.setBookingId(42L);
        issue.setIssueType(com.crms.mvc.enums.IssueType.DAMAGE);
        issue.setStatus(status);
        issue.setSubject("Fine " + id);
        issue.setFeeAmount(new BigDecimal(fee));
        issue.setFeePaid(paid);
        issue.setFeeLocked(paid);
        issue.setOccurredAt(java.time.OffsetDateTime.now().minusDays(3));
        return issue;
    }

    private BookingDetailDTO bookingWithContract(String status, boolean issued) {
        OffsetDateTime pickup = OffsetDateTime.of(2026, 10, 24, 9, 0, 0, 0, ZoneOffset.ofHours(7));
        BookingDetailDTO booking = new BookingDetailDTO();
        booking.setId(42L);
        booking.setCustomerId(1L);
        booking.setCustomerName("Nguyễn Văn An");
        booking.setCustomerPhone("0901234567");
        booking.setCustomerEmail("an@example.com");
        booking.setVehicleTypeId(1L);
        booking.setVehicleTypeName("Vios");
        booking.setVehicleCategory("sedan");
        booking.setSeats(5);
        booking.setImageUrl("/images/vehicles/vios.jpg");
        booking.setLicensePlate("Unassigned");
        booking.setPickupTime(pickup);
        booking.setScheduledEndTime(pickup.plusHours(10));
        booking.setPricePerHour(new BigDecimal("100000"));
        booking.setTotalPrice(new BigDecimal("1000000"));
        booking.setDepositAmount(new BigDecimal("300000"));
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setLateFee(BigDecimal.ZERO);
        booking.setStatus(status);
        booking.setCreatedAt(pickup.minusDays(1));
        booking.setPayments(List.of());
        booking.setFees(List.of());

        ContractDTO contract = new ContractDTO();
        contract.setId(7L);
        contract.setContractNumber("CTR-20261023-0042");
        contract.setAcceptedAt(pickup.minusDays(1));
        contract.setAcceptedIp("127.0.0.1");
        contract.setIssued(issued);
        contract.setIssuedAt(issued ? pickup.minusDays(1).plusMinutes(5) : null);
        contract.setTerms(terms);
        booking.setContract(contract);
        return booking;
    }
}