package com.crms.mvc.controller;

import com.crms.mvc.service.LicenseDocumentService;
import com.crms.mvc.enums.VehicleStatus;
import com.crms.mvc.support.PassThroughSessionAccountService;
import org.springframework.context.annotation.Import;
import com.crms.mvc.support.TestSessions;
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
@Import(PassThroughSessionAccountService.class)
class AdminDashboardVehicleCountTest {
    @MockitoBean private LicenseDocumentService licenseDocumentService; // the global advice reads it on every page
    @Autowired private MockMvc mvc;
    @MockitoBean private VehicleService vehicleService;
    @MockitoBean private VehicleInspectionService vehicleInspectionService;
    @MockitoBean private LicensePlateService licensePlateService;
    @MockitoBean private BookingService bookingService;
    @MockitoBean private com.crms.mvc.service.BookingQueryService bookingQueryService;
    @MockitoBean private com.crms.mvc.service.VehicleAvailabilityService vehicleAvailabilityService;

    @Test
    void retiredVehiclesAreCountedSeparatelyFromTheFleetTotal() throws Exception {
        when(vehicleService.findAll()).thenReturn(List.of(
                vehicle(1L, VehicleStatus.AVAILABLE), vehicle(2L, VehicleStatus.RENTED),
                vehicle(3L, VehicleStatus.MAINTENANCE), vehicle(4L, VehicleStatus.RETIRED)));

        mvc.perform(get("/admin/dashboard").session(admin()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("totalVehicles", 3L))
                .andExpect(model().attribute("retiredVehicles", 1L))
                .andExpect(content().string(containsString("xe ngừng khai thác")));
    }

    @Test
    void theRegistrationAlertNamesTheCarsThatAreExpiringAndExpired() throws Exception {
        java.time.LocalDate today = java.time.LocalDate.now();
        when(licensePlateService.findAllOverviews()).thenReturn(List.of(
                overview(1L, "Toyota Vios", "available", "51A-111.11", today.plusDays(12)),
                overview(2L, "Honda CR-V", "available", "51A-222.22", today.minusDays(3)),
                overview(3L, "Kia Morning", "available", "51A-333.33", today.plusDays(2)),
                overview(4L, "Ford Ranger", "retired", "51A-444.44", today.minusDays(40)),
                overview(5L, "Mazda 3", "available", "51A-555.55", today.plusYears(1))));

        mvc.perform(get("/admin/dashboard").session(admin()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("expiringSoonCount", 2))
                .andExpect(model().attribute("expiredCount", 1))
                .andExpect(content().string(containsString("Honda CR-V")))
                .andExpect(content().string(containsString("quá hạn 3 ngày")))
                .andExpect(content().string(containsString("Toyota Vios")))
                .andExpect(content().string(containsString("còn 12 ngày")))
                .andExpect(content().string(containsString("/admin/inspections/new?vehicleId=3")))
                // a retired car is out of service for good, and a car with a year left is not an alert
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Ford Ranger"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Mazda 3"))));
    }

    private static com.crms.mvc.dto.LicensePlateOverviewDTO overview(Long id, String name, String status, String plate,
                                                                    java.time.LocalDate expiry) {
        return new com.crms.mvc.dto.LicensePlateOverviewDTO(id, name, status, plate, "TP. Hồ Chí Minh", null, expiry);
    }

    private static Vehicle vehicle(Long id, VehicleStatus status) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setStatus(status);
        return vehicle;
    }

    private static MockHttpSession admin() {
        return TestSessions.admin(1L);
    }
}
