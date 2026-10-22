package com.crms.mvc.controller;

import com.crms.mvc.dto.VehicleDTO;
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

@WebMvcTest(VehicleController.class)
class VehicleControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private VehicleService vehicleService;
    @MockitoBean private VehicleTypeService vehicleTypeService;

    private final VehicleDTO vehicle = new VehicleDTO(1L, "51A-123.45", "Toyota", "Vios", 2025, new BigDecimal("180000"), "available");

    @BeforeEach
    void setup() {
        when(vehicleTypeService.getDistinctBrands()).thenReturn(List.of("Toyota", "Honda"));
        when(vehicleTypeService.getDistinctCategories()).thenReturn(List.of("sedan", "suv"));
    }

    @Test void listsDatabaseResultsThroughThymeleaf() throws Exception {
        when(vehicleService.searchWithFilter(any())).thenReturn(List.of(vehicle));
        mvc.perform(get("/vehicles").param("keyword", "Toyota"))
                .andExpect(status().isOk()).andExpect(view().name("vehicles/list"))
                .andExpect(content().string(containsString("51A-123.45")))
                .andExpect(content().string(containsString("/vehicles/1")));
    }
    @Test void emptySearchRendersEmptyState() throws Exception {
        when(vehicleService.searchWithFilter(any())).thenReturn(List.of());
        mvc.perform(get("/vehicles").param("keyword", "missing"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Không tìm thấy xe phù hợp")));
    }
    @Test void searchInputIsEscaped() throws Exception {
        String input = "<script>alert(1)</script>";
        when(vehicleService.searchWithFilter(any())).thenReturn(List.of());
        mvc.perform(get("/vehicles").param("keyword", input))
                .andExpect(status().isOk()).andExpect(content().string(not(containsString(input))));
    }
    @Test void showsVehicleDetail() throws Exception {
        when(vehicleService.findById(1L)).thenReturn(Optional.of(vehicle));
        mvc.perform(get("/vehicles/1")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Toyota Vios")));
    }
    @Test void unknownVehicleReturns404() throws Exception {
        when(vehicleService.findById(99L)).thenReturn(Optional.empty());
        mvc.perform(get("/vehicles/99")).andExpect(status().isNotFound());
    }
}
