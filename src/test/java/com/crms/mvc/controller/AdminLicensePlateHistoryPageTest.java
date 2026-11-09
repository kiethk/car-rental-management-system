package com.crms.mvc.controller;

import com.crms.mvc.dto.VehicleDTO;
import com.crms.mvc.service.LicensePlateService;
import com.crms.mvc.service.VehicleService;
import com.crms.mvc.support.PassThroughSessionAccountService;
import com.crms.mvc.support.TestSessions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** The plate history page is built from the vehicle's DTO, not from the entity. */
@WebMvcTest(AdminLicensePlateController.class)
@Import(PassThroughSessionAccountService.class)
class AdminLicensePlateHistoryPageTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private LicensePlateService licensePlateService;
    @MockitoBean private VehicleService vehicleService;

    @Test
    void theTitleNamesTheCarAndItsType() throws Exception {
        VehicleDTO car = new VehicleDTO();
        car.setId(5L);
        car.setBrand("Toyota");
        car.setModel("Vios");
        when(vehicleService.findVehicleById(5L)).thenReturn(Optional.of(car));
        when(licensePlateService.findHistory(5L)).thenReturn(List.of());

        mvc.perform(get("/admin/license-plates/vehicle/5").session(TestSessions.staff(2L)))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/license-plates/history"))
                .andExpect(content().string(containsString("xe #5 (Toyota Vios)")));
    }
}
