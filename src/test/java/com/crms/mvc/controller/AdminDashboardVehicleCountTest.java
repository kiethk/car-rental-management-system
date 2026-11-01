package com.crms.mvc.controller;

import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.service.BookingService;
import com.crms.mvc.service.LicensePlateService;
import com.crms.mvc.service.VehicleInspectionService;
import com.crms.mvc.service.VehicleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Retired vehicles are out of service for good, so the fleet total counts only the cars that can still work. */
@WebMvcTest(AdminDashboardController.class)
class AdminDashboardVehicleCountTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private VehicleService vehicleService;
    @MockitoBean private VehicleInspectionService vehicleInspectionService;
    @MockitoBean private LicensePlateService licensePlateService;
    @MockitoBean private BookingService bookingService;

    @Test
    void retiredVehiclesAreCountedSeparatelyFromTheFleetTotal() throws Exception {
        when(vehicleService.findAll()).thenReturn(List.of(
                vehicle(1L, Vehicle.STATUS_AVAILABLE), vehicle(2L, Vehicle.STATUS_RENTED),
                vehicle(3L, Vehicle.STATUS_MAINTENANCE), vehicle(4L, Vehicle.STATUS_RETIRED)));

        mvc.perform(get("/admin/dashboard").session(admin()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("totalVehicles", 3L))
                .andExpect(model().attribute("retiredVehicles", 1L))
                .andExpect(content().string(containsString("xe ngừng khai thác")));
    }

    private static Vehicle vehicle(Long id, String status) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setStatus(status);
        return vehicle;
    }

    private static MockHttpSession admin() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("employeeId", 1L);
        session.setAttribute("employeeRole", "admin");
        return session;
    }
}
