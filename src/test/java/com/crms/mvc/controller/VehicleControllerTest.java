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
                .andExpect(status().isOk()).andExpect(content().string(containsString("No cars match your criteria")));
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
                .andExpect(content().string(containsString("40% Deposit")))
                .andExpect(content().string(containsString("Customer Reviews")))
                .andExpect(content().string(containsString("No reviews yet")))
                .andExpect(content().string(containsString("at least 48 hours")));
    }

    @Test void vehicleDetailShowsReviewsForItsVehicleType() throws Exception {
        when(vehicleService.findById(1L)).thenReturn(Optional.of(vehicle));
        when(reviewService.findPublicReviewsForVehicleType(1L)).thenReturn(
                new VehicleReviewSummaryDTO(new BigDecimal("4.5"),
                        List.of(new ReviewDTO(5, "Excellent service", null),
                                new ReviewDTO(4, "Clean car", null))));

        mvc.perform(get("/vehicles/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("4.5 / 5 · 2 reviews")))
                .andExpect(content().string(containsString("Excellent service")))
                .andExpect(content().string(containsString("Clean car")));
    }
    @Test void unknownVehicleReturns404() throws Exception {
        when(vehicleService.findById(99L)).thenReturn(Optional.empty());
        mvc.perform(get("/vehicles/99")).andExpect(status().isNotFound());
    }
}
