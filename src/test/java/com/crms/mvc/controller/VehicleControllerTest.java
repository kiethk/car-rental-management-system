package com.crms.mvc.controller;

import org.springframework.context.annotation.Import;
import com.crms.mvc.support.PassThroughSessionAccountService;
import com.crms.mvc.dto.ContractTemplateDTO;
import com.crms.mvc.dto.ReviewDTO;
import com.crms.mvc.dto.VehicleDTO;
import com.crms.mvc.dto.VehicleReviewSummaryDTO;
import com.crms.mvc.service.ContractTemplateService;
import com.crms.mvc.service.ReviewService;
import com.crms.mvc.service.VehicleService;
import com.crms.mvc.service.VehicleTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(PassThroughSessionAccountService.class)
@WebMvcTest(VehicleController.class)
class VehicleControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private VehicleService vehicleService;
    @MockitoBean private VehicleTypeService vehicleTypeService;
    @MockitoBean private ContractTemplateService contractTemplateService;
    @MockitoBean private com.crms.mvc.service.BookingService bookingService;
    @MockitoBean private ReviewService reviewService;

    private final VehicleDTO vehicle = new VehicleDTO(1L, "51A-123.45", "Toyota", "Vios", 2025, new BigDecimal("180000"), "available");

    @BeforeEach
    void setup() {
        when(vehicleTypeService.getDistinctBrands()).thenReturn(List.of("Toyota", "Honda"));
        when(vehicleTypeService.getDistinctCategories()).thenReturn(List.of("sedan", "suv"));
        vehicle.setVehicleTypeId(1L);

        ContractTemplateDTO terms = new ContractTemplateDTO();
        terms.setDepositRate(new BigDecimal("0.4000"));
        terms.setLateFeeMultiplier(new BigDecimal("2.00"));
        terms.setFreeCancellationHours(48);
        when(contractTemplateService.getActiveTemplate()).thenReturn(terms);
    }

    @Test void listsDatabaseResultsThroughThymeleaf() throws Exception {
        when(vehicleService.searchWithFilter(any())).thenReturn(List.of(vehicle));
        mvc.perform(get("/vehicles").param("keyword", "Toyota"))
                .andExpect(status().isOk()).andExpect(view().name("vehicles/list"))
                .andExpect(content().string(containsString("Toyota Vios")))
                .andExpect(content().string(containsString("/vehicles/1")));
    }
    @Test void emptySearchRendersEmptyState() throws Exception {
        when(vehicleService.searchWithFilter(any())).thenReturn(List.of());
        mvc.perform(get("/vehicles").param("keyword", "missing"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Không có xe phù hợp")));
    }
    @Test void searchInputIsEscaped() throws Exception {
        String input = "<script>alert(1)</script>";
        when(vehicleService.searchWithFilter(any())).thenReturn(List.of());
        mvc.perform(get("/vehicles").param("keyword", input))
                .andExpect(status().isOk()).andExpect(content().string(not(containsString(input))));
    }
    @Test void showsVehicleDetail() throws Exception {
        when(vehicleService.findById(1L)).thenReturn(Optional.of(vehicle));
        when(reviewService.findPublicReviewsForVehicleType(1L))
                .thenReturn(new VehicleReviewSummaryDTO(BigDecimal.ZERO, List.of()));
        mvc.perform(get("/vehicles/1")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Toyota Vios")))
                .andExpect(content().string(containsString("Đánh giá của khách hàng")))
                .andExpect(content().string(containsString("Chưa có đánh giá")))
                .andExpect(content().string(containsString("ít nhất 48 giờ")));
    }
    @Test void scheduleShowsBookedAndBufferHoursAndBooksThatCar() throws Exception {
        java.util.List<com.crms.mvc.dto.VehicleScheduleDTO.HourState> hours =
                new java.util.ArrayList<>(java.util.Collections.nCopies(24, com.crms.mvc.dto.VehicleScheduleDTO.HourState.FREE));
        hours.set(9, com.crms.mvc.dto.VehicleScheduleDTO.HourState.BUFFER);
        hours.set(10, com.crms.mvc.dto.VehicleScheduleDTO.HourState.BOOKED);
        com.crms.mvc.dto.VehicleScheduleDTO schedule = new com.crms.mvc.dto.VehicleScheduleDTO();
        schedule.setVehicleId(5L);
        schedule.setLicensePlate("51A-123.45");
        schedule.setVehicleTypeId(1L);
        schedule.setVehicleTypeName("Toyota Vios");
        schedule.setVehicleStatus("available");
        schedule.setDays(List.of(new com.crms.mvc.dto.VehicleScheduleDTO.Day(java.time.LocalDate.of(2026, 11, 10), hours)));
        when(bookingService.getVehicleSchedule(eq(5L), any(), anyInt())).thenReturn(schedule);

        mvc.perform(get("/vehicles/cars/5/schedule")).andExpect(status().isOk())
                .andExpect(view().name("vehicles/schedule"))
                .andExpect(content().string(containsString("51A-123.45")))
                .andExpect(content().string(containsString("sch-cell sch-booked")))
                .andExpect(content().string(containsString("sch-cell sch-buffer")))
                .andExpect(content().string(containsString("name=\"vehicleId\" value=\"5\"")))
                .andExpect(content().string(containsString("Đặt xe này")));
    }
    @Test void retiredCarHasNoSchedule() throws Exception {
        com.crms.mvc.dto.VehicleScheduleDTO schedule = new com.crms.mvc.dto.VehicleScheduleDTO();
        schedule.setVehicleStatus("retired");
        schedule.setDays(List.of());
        when(bookingService.getVehicleSchedule(eq(6L), any(), anyInt())).thenReturn(schedule);
        mvc.perform(get("/vehicles/cars/6/schedule")).andExpect(status().isNotFound());
    }

    @Test void vehicleDetailShowsReviewsForItsVehicleType() throws Exception {
        when(vehicleService.findById(1L)).thenReturn(Optional.of(vehicle));
        when(reviewService.findPublicReviewsForVehicleType(1L)).thenReturn(
                new VehicleReviewSummaryDTO(new BigDecimal("4.5"),
                        List.of(new ReviewDTO(5, "Excellent service", null),
                                new ReviewDTO(4, "Clean car", null))));

        mvc.perform(get("/vehicles/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("4.5 / 5 · 2 đánh giá")))
                .andExpect(content().string(containsString("Excellent service")))
                .andExpect(content().string(containsString("Clean car")));
    }
    @Test void unknownVehicleReturns404() throws Exception {
        when(vehicleService.findById(99L)).thenReturn(Optional.empty());
        mvc.perform(get("/vehicles/99")).andExpect(status().isNotFound());
    }
}
