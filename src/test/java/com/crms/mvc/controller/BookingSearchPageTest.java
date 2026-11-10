package com.crms.mvc.controller;

import com.crms.mvc.dto.VehicleTypeSearchResultDTO;
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
import com.crms.mvc.support.PassThroughSessionAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Characterization of GET /search: how the rental window is read from the (legacy and current) query parameters,
 * how the filters narrow the results, and which model attributes the page reads. These must survive the refactor.
 */
@Import(PassThroughSessionAccountService.class)
@WebMvcTest(BookingController.class)
class BookingSearchPageTest {
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

    @BeforeEach
    void setUp() {
        when(vehicleAvailabilityService.searchAvailableVehicleTypes(any(), any(), any())).thenReturn(List.of(
                result(1L, "Vios", "Toyota", "sedan", 5, "automatic", "100000"),
                result(2L, "Fortuner", "Toyota", "suv", 7, "manual", "300000"),
                result(3L, "Mazda3", "Mazda", "sedan", 5, "automatic", "150000")));
        when(reviewService.ratingsByVehicleType()).thenReturn(Map.of());
    }

    @Test
    void withoutParametersTheWindowIsTomorrowNineToSix() throws Exception {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        mvc.perform(get("/search"))
                .andExpect(status().isOk())
                .andExpect(view().name("bookings/search"))
                .andExpect(model().attribute("startTime", LocalDateTime.of(tomorrow, java.time.LocalTime.of(9, 0))))
                .andExpect(model().attribute("endTime", LocalDateTime.of(tomorrow, java.time.LocalTime.of(18, 0))))
                .andExpect(model().attribute("selectedStartDate", tomorrow.toString()))
                .andExpect(model().attribute("selectedEndDate", tomorrow.toString()))
                .andExpect(model().attribute("selectedStart", "09:00"))
                .andExpect(model().attribute("selectedEnd", "18:00"))
                .andExpect(model().attribute("rentalHours", 9L))
                .andExpect(model().attribute("durationDisplay", "9h"));
    }

    @Test
    void startAndEndDatesWithTimesAreUsedAsGiven() throws Exception {
        mvc.perform(get("/search")
                        .param("startDate", "2030-01-10").param("endDate", "2030-01-12")
                        .param("startTime", "08:00").param("endTime", "20:00"))
                .andExpect(model().attribute("startTime", LocalDateTime.of(2030, 1, 10, 8, 0)))
                .andExpect(model().attribute("endTime", LocalDateTime.of(2030, 1, 12, 20, 0)))
                .andExpect(model().attribute("rentalHours", 60L))
                .andExpect(model().attribute("rentalDays", 2L))
                .andExpect(model().attribute("remainingHours", 12L))
                .andExpect(model().attribute("durationDisplay", "2d 12h"));
    }

    @Test
    void theLegacyDateParameterSetsBothDays() throws Exception {
        mvc.perform(get("/search").param("date", "2030-02-01"))
                .andExpect(model().attribute("selectedStartDate", "2030-02-01"))
                .andExpect(model().attribute("selectedEndDate", "2030-02-01"))
                .andExpect(model().attribute("selectedDate", "2030-02-01"));
    }

    @Test
    void aStartDateAloneAlsoEndsThatDay() throws Exception {
        mvc.perform(get("/search").param("startDate", "2030-03-05"))
                .andExpect(model().attribute("selectedStartDate", "2030-03-05"))
                .andExpect(model().attribute("selectedEndDate", "2030-03-05"));
    }

    @Test
    void fullDateTimeValuesInTheTimeParametersWinOverTheSeparateDates() throws Exception {
        mvc.perform(get("/search")
                        .param("startDate", "2030-01-01")
                        .param("startTime", "2030-04-02T10:00").param("endTime", "2030-04-03T12:00"))
                .andExpect(model().attribute("startTime", LocalDateTime.of(2030, 4, 2, 10, 0)))
                .andExpect(model().attribute("endTime", LocalDateTime.of(2030, 4, 3, 12, 0)))
                .andExpect(model().attribute("selectedStartDate", "2030-04-02"))
                .andExpect(model().attribute("selectedEndDate", "2030-04-03"));
    }

    @Test
    void anUnreadableDateSilentlyFallsBackToTomorrow() throws Exception {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        mvc.perform(get("/search").param("startDate", "not-a-date").param("startTime", "nine"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedStartDate", tomorrow.toString()))
                .andExpect(model().attribute("selectedStart", "09:00"));
    }

    @Test
    void anInvalidWindowIsReportedAndNothingIsSearched() throws Exception {
        doThrow(new IllegalArgumentException("Giờ trả xe phải sau giờ nhận xe"))
                .when(bookingService).validateRentalSchedule(any(), any());

        MvcResult result = mvc.perform(get("/search")
                        .param("startDate", "2030-01-10").param("startTime", "18:00").param("endTime", "09:00"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("scheduleError", "Giờ trả xe phải sau giờ nhận xe"))
                .andReturn();

        assertThat((List<?>) result.getModelAndView().getModel().get("results")).isEmpty();
        verify(vehicleAvailabilityService, never()).searchAvailableVehicleTypes(any(), any(), any());
    }

    @Test
    void theWindowIsSearchedInVietnamTime() throws Exception {
        mvc.perform(get("/search")
                .param("startDate", "2030-01-10").param("startTime", "08:00").param("endTime", "10:00")
                .param("category", "suv"));

        ArgumentCaptor<OffsetDateTime> start = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> end = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(vehicleAvailabilityService).searchAvailableVehicleTypes(start.capture(), end.capture(), eq("suv"));
        assertThat(start.getValue()).isEqualTo(OffsetDateTime.of(2030, 1, 10, 8, 0, 0, 0, VN));
        assertThat(end.getValue()).isEqualTo(OffsetDateTime.of(2030, 1, 10, 10, 0, 0, 0, VN));
    }

    @Test
    void filtersNarrowTheResults() throws Exception {
        assertThat(resultNames(get("/search").param("keyword", "forTUN"))).containsExactly("Fortuner");
        assertThat(resultNames(get("/search").param("brand", "mazda"))).containsExactly("Mazda3");
        assertThat(resultNames(get("/search").param("transmission", "manual"))).containsExactly("Fortuner");
        assertThat(resultNames(get("/search").param("seats", "7"))).containsExactly("Fortuner");
        assertThat(resultNames(get("/search").param("maxPrice", "120000"))).containsExactly("Vios");
        assertThat(resultNames(get("/search").param("brand", "all").param("transmission", "all")))
                .containsExactly("Vios", "Fortuner", "Mazda3");
    }

    @Test
    void theModelCarriesEveryAttributeThePageReads() throws Exception {
        MvcResult result = mvc.perform(get("/search")).andExpect(status().isOk()).andReturn();

        assertThat(result.getModelAndView().getModel().keySet()).contains(
                "scheduleError", "today", "selectedStartDate", "selectedEndDate", "selectedDate", "selectedStart",
                "selectedEnd", "startTime", "endTime", "keyword", "brand", "category", "minSeats", "transmission",
                "maxPrice", "sortBy", "rentalHours", "rentalDays", "remainingHours", "durationDisplay", "results",
                "ratings", "allCount", "brands", "categories");
    }

    @Test
    void theLegacyVehiclesAvailableUrlShowsTheSamePage() throws Exception {
        mvc.perform(get("/vehicles/available")).andExpect(status().isOk()).andExpect(view().name("bookings/search"));
    }

    @SuppressWarnings("unchecked")
    private List<String> resultNames(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        MvcResult result = mvc.perform(request).andExpect(status().isOk()).andReturn();
        return ((List<VehicleTypeSearchResultDTO>) result.getModelAndView().getModel().get("results"))
                .stream().map(VehicleTypeSearchResultDTO::getName).toList();
    }

    private static VehicleTypeSearchResultDTO result(Long id, String name, String brand, String category, int seats,
                                                     String transmission, String price) {
        VehicleTypeSearchResultDTO dto = new VehicleTypeSearchResultDTO();
        dto.setId(id);
        dto.setName(name);
        dto.setBrand(brand);
        dto.setCategory(category);
        dto.setSeats(seats);
        dto.setTransmission(transmission);
        dto.setPricePerHour(new BigDecimal(price));
        dto.setAvailableCount(2);
        return dto;
    }
}
