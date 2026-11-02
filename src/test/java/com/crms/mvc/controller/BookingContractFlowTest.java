package com.crms.mvc.controller;

import com.crms.mvc.support.TestSessions;
import org.springframework.context.annotation.Import;
import com.crms.mvc.support.PassThroughSessionAccountService;
import com.crms.mvc.dto.BookingDetailDTO;
import com.crms.mvc.dto.ContractDTO;
import com.crms.mvc.dto.ContractTemplateDTO;
import com.crms.mvc.entity.ContractTemplateStatus;
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
    @MockitoBean private PaymentService paymentService;
    @MockitoBean private VehicleTypeService vehicleTypeService;
    @MockitoBean private PricingService pricingService;
    @MockitoBean private LicenseDocumentService licenseDocumentService;
    @MockitoBean private ContractTemplateService contractTemplateService;
    @MockitoBean private com.crms.mvc.service.IssueService issueService;

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
        when(licenseDocumentService.hasValidLicense(eq(1L), any())).thenReturn(true);
        when(pricingService.calculatePrice(any(), any(), any(), any(), any())).thenAnswer(inv -> new PricingServiceImpl().calculatePrice(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2), inv.getArgument(3), inv.getArgument(4)));
    }

    @Test
    void checkoutShowsActiveTermsVersionAndDepositFromIt() throws Exception {
        VehicleType type = new VehicleType();
        type.setId(1L);
        type.setName("Vios");
        type.setBrand("Toyota");
        type.setCategory("sedan");
        type.setSeats(5);
        type.setTransmission("automatic");
        type.setFuelType("gasoline");
        type.setPricePerHour(new BigDecimal("100000"));
        LicensePlate plate = new LicensePlate();
        plate.setPlate("51A-123.45");
        Vehicle car = new Vehicle();
        car.setId(9L);
        car.setColor("White");
        car.setCurrentPlate(plate);
        car.setVehicleType(type);
        when(vehicleTypeService.findById(1L)).thenReturn(Optional.of(type));
        when(bookingService.findAvailableVehiclesInTimeRange(eq(1L), any(), any())).thenReturn(List.of(car));

        String html = mvc.perform(get("/bookings/checkout").session(customerSession)
                        .param("vehicleTypeId", "1")
                        .param("startTime", "2026-10-24T09:00")
                        .param("endTime", "2026-10-24T19:00"))
                .andExpect(status().isOk())
                .andExpect(view().name("bookings/checkout"))
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
                .andExpect(flash().attribute("error", "You must accept the rental terms to continue"));

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
    void bookingDetailOffersPdfOnceIssued() throws Exception {
        when(bookingService.getBookingDetail(42L, 1L)).thenReturn(bookingWithContract("confirmed", true));

        mvc.perform(get("/bookings/42").session(customerSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/bookings/42/contract")))
                .andExpect(content().string(containsString("CTR-20261023-0042")))
                .andExpect(content().string(containsString("30%")))
                .andExpect(content().string(containsString("150%")));
    }

    @Test
    void depositPageUsesTermsVersionAcceptedAtCheckout() throws Exception {
        when(bookingService.getBookingDetail(42L, 1L)).thenReturn(bookingWithContract("pending_payment", false));

        mvc.perform(get("/bookings/42/pay-deposit").session(customerSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("30%")))
                .andExpect(content().string(containsString("48")))
                .andExpect(content().string(not(containsString("/bookings/42/contract\""))));
    }

    @Test
    void successPageShowsDepositPercentOfAcceptedTerms() throws Exception {
        when(bookingService.getBookingDetail(42L, 1L)).thenReturn(bookingWithContract("confirmed", true));

        mvc.perform(get("/bookings/42/success").session(customerSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("30%, VNPay")));
    }

    @Test
    void customerSeesTheIssuesChargedOnTheirBooking() throws Exception {
        when(bookingService.getBookingDetail(42L, 1L)).thenReturn(bookingWithContract("completed", true));
        com.crms.mvc.dto.IssueDTO issue = new com.crms.mvc.dto.IssueDTO();
        issue.setId(7L);
        issue.setBookingId(42L);
        issue.setIssueType(com.crms.mvc.entity.IssueType.DAMAGE);
        issue.setStatus(com.crms.mvc.entity.IssueStatus.RESOLVED);
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
    void aBookingWithoutIssuesHasNoIssueSection() throws Exception {
        when(bookingService.getBookingDetail(42L, 1L)).thenReturn(bookingWithContract("confirmed", true));
        when(issueService.findByBookingForCustomer(42L, 1L)).thenReturn(List.of());

        mvc.perform(get("/bookings/42").session(customerSession))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Sự cố trong chuyến thuê"))));
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
        when(bookingService.getBookingDetail(42L, 1L)).thenReturn(bookingWithContract("completed", true));
        when(issueService.findByBookingForCustomer(42L, 1L)).thenReturn(List.of(
                issueWith(7L, com.crms.mvc.entity.IssueStatus.OPEN, "200000", false),
                issueWith(8L, com.crms.mvc.entity.IssueStatus.OPEN, "500000", false),
                issueWith(9L, com.crms.mvc.entity.IssueStatus.RESOLVED, "999000", true),
                issueWith(10L, com.crms.mvc.entity.IssueStatus.CANCELLED, "300000", false)));

        mvc.perform(get("/bookings/42").session(customerSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Phí sự cố chưa thanh toán")))
                .andExpect(content().string(containsString("/bookings/42/pay-issue-fees")))
                // only open and unpaid: 200,000 + 500,000; the resolved and cancelled ones are left out
                .andExpect(content().string(containsString("700,000")));
    }

    @Test
    void nothingIsOfferedToPayWhenEverythingIsPaidOrCancelled() throws Exception {
        when(bookingService.getBookingDetail(42L, 1L)).thenReturn(bookingWithContract("completed", true));
        when(issueService.findByBookingForCustomer(42L, 1L)).thenReturn(List.of(
                issueWith(9L, com.crms.mvc.entity.IssueStatus.CANCELLED, "999000", false),
                issueWith(10L, com.crms.mvc.entity.IssueStatus.RESOLVED, "300000", true)));

        mvc.perform(get("/bookings/42").session(customerSession))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("/bookings/42/pay-issue-fees"))));
    }

    @Test
    void feesArePayableOnlineDuringActiveRental() throws Exception {
        when(bookingService.getBookingDetail(42L, 1L)).thenReturn(bookingWithContract("active", true));
        when(issueService.findByBookingForCustomer(42L, 1L)).thenReturn(List.of(
                issueWith(7L, com.crms.mvc.entity.IssueStatus.OPEN, "200000", false)));

        mvc.perform(get("/bookings/42").session(customerSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/bookings/42/pay-issue-fees")));
    }

    @Test
    void aPaymentLeftPendingCanBeRetried() throws Exception {
        when(bookingService.getBookingDetail(42L, 1L)).thenReturn(bookingWithContract("completed", true));
        com.crms.mvc.dto.IssueDTO pending = issueWith(7L, com.crms.mvc.entity.IssueStatus.OPEN, "200000", false);
        pending.setFeeLocked(true); // a payment has taken it but has not been paid yet
        when(issueService.findByBookingForCustomer(42L, 1L)).thenReturn(List.of(pending));

        mvc.perform(get("/bookings/42").session(customerSession))
                .andExpect(content().string(containsString("/bookings/42/pay-issue-fees")));
    }

    private com.crms.mvc.dto.IssueDTO issueWith(Long id, com.crms.mvc.entity.IssueStatus status, String fee, boolean paid) {
        com.crms.mvc.dto.IssueDTO issue = new com.crms.mvc.dto.IssueDTO();
        issue.setId(id);
        issue.setBookingId(42L);
        issue.setIssueType(com.crms.mvc.entity.IssueType.TRAFFIC_FINE);
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