package com.crms.mvc.controller;

import com.crms.mvc.dto.ContractTemplateDTO;
import com.crms.mvc.entity.ContractTemplateStatus;
import com.crms.mvc.entity.LicensePlate;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.entity.VehicleType;
import com.crms.mvc.service.BookingService;
import com.crms.mvc.service.ContractTemplateService;
import com.crms.mvc.service.HandoverService;
import com.crms.mvc.service.IssueService;
import com.crms.mvc.service.LicenseDocumentService;
import com.crms.mvc.service.PaymentService;
import com.crms.mvc.service.PricingService;
import com.crms.mvc.service.ReviewService;
import com.crms.mvc.service.TrafficFineService;
import com.crms.mvc.service.VehicleTypeService;
import com.crms.mvc.service.impl.PricingServiceImpl;
import com.crms.mvc.support.PassThroughSessionAccountService;
import com.crms.mvc.support.TestSessions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Characterization of GET /bookings/checkout: the guards in front of the page, how the window is read, and the model
 * the page reads. These must survive the refactor of the controller.
 */
@Import(PassThroughSessionAccountService.class)
@WebMvcTest(BookingController.class)
class BookingCheckoutPageTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private BookingService bookingService;
    @MockitoBean private com.crms.mvc.service.BookingQueryService bookingQueryService;
    @MockitoBean private com.crms.mvc.service.VehicleAvailabilityService vehicleAvailabilityService;
    @MockitoBean private PaymentService paymentService;
    @MockitoBean private VehicleTypeService vehicleTypeService;
    @MockitoBean private PricingService pricingService;
    @MockitoBean private LicenseDocumentService licenseDocumentService;
    @MockitoBean private ContractTemplateService contractTemplateService;
    @MockitoBean private IssueService issueService;
    @MockitoBean private HandoverService handoverService;
    @MockitoBean private ReviewService reviewService;
    @MockitoBean private TrafficFineService trafficFineService;

    private static final ZoneOffset VN = ZoneOffset.ofHours(7);
    private MockHttpSession customer;
    private VehicleType type;
    private Vehicle car;

    @BeforeEach
    void setUp() {
        customer = TestSessions.customer(1L, "Nguyễn Văn An");

        ContractTemplateDTO terms = new ContractTemplateDTO();
        terms.setId(3L);
        terms.setVersionNo(3);
        terms.setTitle("Hợp đồng thuê xe ô tô tự lái");
        terms.setTermsContent("1. Không hút thuốc trong xe.");
        terms.setDepositRate(new BigDecimal("0.3000"));
        terms.setLateFeeMultiplier(new BigDecimal("1.50"));
        terms.setFreeCancellationHours(48);
        terms.setStatus(ContractTemplateStatus.ACTIVE);
        when(contractTemplateService.getActiveTemplate()).thenReturn(terms);
        when(pricingService.calculatePrice(any(), any(), any(), any(), any())).thenAnswer(inv ->
                new PricingServiceImpl().calculatePrice(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2),
                        inv.getArgument(3), inv.getArgument(4)));

        type = new VehicleType();
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
        car = new Vehicle();
        car.setId(9L);
        car.setColor("White");
        car.setCurrentPlate(plate);
        car.setVehicleType(type);
        when(vehicleTypeService.findById(1L)).thenReturn(Optional.of(type));
        when(vehicleAvailabilityService.findAvailableVehiclesInTimeRange(eq(1L), any(), any())).thenReturn(List.of(car));
    }

    @Test
    void aVisitorWhoIsNotLoggedInIsSentToTheLoginPage() throws Exception {
        mvc.perform(get("/bookings/checkout").param("vehicleTypeId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/login*"));
    }

    @Test
    void anUnknownVehicleTypeGoesBackToTheSearchPage() throws Exception {
        mvc.perform(get("/bookings/checkout").session(customer).param("vehicleTypeId", "99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/search"))
                .andExpect(flash().attribute("error", "Không tìm thấy dòng xe."));
    }

    @Test
    void anInvalidWindowGoesBackToTheSearchPageWithTheReason() throws Exception {
        doThrow(new IllegalArgumentException("Giờ trả xe phải sau giờ nhận xe"))
                .when(bookingService).validateRentalSchedule(any(), any());

        mvc.perform(get("/bookings/checkout").session(customer).param("vehicleTypeId", "1")
                        .param("startTime", "2030-01-10T18:00").param("endTime", "2030-01-10T09:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/search"))
                .andExpect(flash().attribute("error", "Giờ trả xe phải sau giờ nhận xe"));
    }

    @Test
    void whenNoCarOfTheTypeIsFreeTheCustomerIsSentBackToSearch() throws Exception {
        when(vehicleAvailabilityService.findAvailableVehiclesInTimeRange(eq(1L), any(), any())).thenReturn(List.of());

        mvc.perform(get("/bookings/checkout").session(customer).param("vehicleTypeId", "1")
                        .param("startTime", "2030-01-10T09:00").param("endTime", "2030-01-10T19:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/search"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void separateDatesAndTimesAreCombinedInVietnamTime() throws Exception {
        mvc.perform(get("/bookings/checkout").session(customer).param("vehicleTypeId", "1")
                        .param("startDate", "2030-01-10").param("endDate", "2030-01-11")
                        .param("startTime", "08:00").param("endTime", "20:00"))
                .andExpect(status().isOk());

        ArgumentCaptor<OffsetDateTime> start = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> end = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(vehicleAvailabilityService).findAvailableVehiclesInTimeRange(eq(1L), start.capture(), end.capture());
        assertThat(start.getValue()).isEqualTo(OffsetDateTime.of(2030, 1, 10, 8, 0, 0, 0, VN));
        assertThat(end.getValue()).isEqualTo(OffsetDateTime.of(2030, 1, 11, 20, 0, 0, 0, VN));
    }

    @Test
    void withoutTimesTheWindowDefaultsToNineToSix() throws Exception {
        mvc.perform(get("/bookings/checkout").session(customer).param("vehicleTypeId", "1")
                        .param("date", "2030-05-05"))
                .andExpect(status().isOk());

        ArgumentCaptor<OffsetDateTime> start = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> end = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(vehicleAvailabilityService).findAvailableVehiclesInTimeRange(eq(1L), start.capture(), end.capture());
        assertThat(start.getValue()).isEqualTo(OffsetDateTime.of(2030, 5, 5, 9, 0, 0, 0, VN));
        assertThat(end.getValue()).isEqualTo(OffsetDateTime.of(2030, 5, 5, 18, 0, 0, 0, VN));
    }

    @Test
    void anUnreadableDateFallsBackToTomorrowInsteadOfFailing() throws Exception {
        mvc.perform(get("/bookings/checkout").session(customer).param("vehicleTypeId", "1")
                        .param("startDate", "not-a-date").param("startTime", "nine"))
                .andExpect(status().isOk())
                .andExpect(view().name("bookings/checkout"));

        ArgumentCaptor<OffsetDateTime> start = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(bookingService).validateRentalSchedule(any(), any());
        verify(vehicleAvailabilityService).findAvailableVehiclesInTimeRange(eq(1L), start.capture(), any());
        assertThat(start.getValue().toLocalTime()).isEqualTo(java.time.LocalTime.of(9, 0));
    }

    @Test
    void thePreselectedCarIsKeptOnlyWhenItIsFree() throws Exception {
        MvcResult free = mvc.perform(get("/bookings/checkout").session(customer).param("vehicleTypeId", "1")
                        .param("vehicleId", "9")
                        .param("startTime", "2030-01-10T09:00").param("endTime", "2030-01-10T19:00"))
                .andExpect(status().isOk()).andReturn();
        MvcResult taken = mvc.perform(get("/bookings/checkout").session(customer).param("vehicleTypeId", "1")
                        .param("vehicleId", "77")
                        .param("startTime", "2030-01-10T09:00").param("endTime", "2030-01-10T19:00"))
                .andExpect(status().isOk()).andReturn();

        assertThat(free.getModelAndView().getModel().get("error")).isNull();
        assertThat(taken.getModelAndView().getModel().get("error")).asString().contains("không trống");
    }

    @Test
    void theModelCarriesEveryAttributeThePageReads() throws Exception {
        MvcResult result = mvc.perform(get("/bookings/checkout").session(customer).param("vehicleTypeId", "1")
                        .param("startTime", "2030-01-10T09:00").param("endTime", "2030-01-10T19:00"))
                .andExpect(status().isOk())
                .andExpect(view().name("bookings/checkout"))
                .andExpect(model().attribute("rentalHours", 10L))
                .andExpect(model().attribute("durationDisplay", "10h"))
                .andReturn();

        assertThat(result.getModelAndView().getModel().keySet()).contains(
                "vehicleType", "bookingForm", "availableCars", "customerLicense", "hasValidLicense", "licenseExpired",
                "rentalHours", "rentalDays", "remainingHours", "durationDisplay", "estimatedTotal", "depositAmount",
                "remainingAmount", "vehicleImage", "contractTerms");
        LocalDateTime start = LocalDateTime.of(2030, 1, 10, 9, 0);
        assertThat(result.getModelAndView().getModel().get("estimatedTotal")).isEqualTo(new BigDecimal("1000000"));
        assertThat(result.getModelAndView().getModel().get("depositAmount")).isEqualTo(new BigDecimal("300000"));
        assertThat(start).isBefore(LocalDateTime.of(2030, 1, 10, 19, 0));
    }
}
