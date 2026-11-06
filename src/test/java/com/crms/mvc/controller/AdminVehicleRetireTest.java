package com.crms.mvc.controller;

import com.crms.mvc.support.PassThroughSessionAccountService;
import org.springframework.context.annotation.Import;
import com.crms.mvc.support.TestSessions;
import com.crms.mvc.dto.VehicleDTO;
import com.crms.mvc.dto.VehicleFormDTO;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.service.VehicleService;
import com.crms.mvc.service.VehicleTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Retiring a vehicle (a permanent, asset-level decision) is an admin-only action. */
@WebMvcTest(AdminVehicleController.class)
@Import(PassThroughSessionAccountService.class)
class AdminVehicleRetireTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private VehicleService vehicleService;
    @MockitoBean private VehicleTypeService vehicleTypeService;

    private MockHttpSession admin;
    private MockHttpSession staff;

    @BeforeEach
    void setUp() {
        admin = sessionOf("admin");
        staff = sessionOf("staff");

        Vehicle saved = new Vehicle();
        saved.setId(5L);
        when(vehicleService.update(anyLong(), any())).thenReturn(saved);
    }

    @Test
    void adminCanRetireAVehicle() throws Exception {
        mvc.perform(post("/admin/vehicles/5/status").session(admin).param("status", "retired"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));

        verify(vehicleService).updateStatus(5L, "retired");
    }

    @Test
    void staffCannotRetireAVehicle() throws Exception {
        mvc.perform(post("/admin/vehicles/5/status").session(staff).param("status", "retired"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));

        verify(vehicleService, never()).updateStatus(anyLong(), anyString());
    }

    @Test
    void staffCanStillMoveAVehicleToMaintenance() throws Exception {
        mvc.perform(post("/admin/vehicles/5/status").session(staff).param("status", "maintenance"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));

        verify(vehicleService).updateStatus(5L, "maintenance");
    }

    @Test
    void onlyAdminsSeeTheRetireButtonOnWorkingVehicles() throws Exception {
        listOf(car(1, "available"), car(2, "maintenance"));

        mvc.perform(get("/admin/vehicles").session(admin))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ngừng khai thác vĩnh viễn")));
        mvc.perform(get("/admin/vehicles").session(staff))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("ngừng khai thác vĩnh viễn"))));
    }

    @Test
    void aRetiredVehicleOffersNoStatusActions() throws Exception {
        listOf(car(3, "retired"));

        mvc.perform(get("/admin/vehicles").session(admin))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Đã ngừng khai thác")))
                .andExpect(content().string(not(containsString("ngừng khai thác vĩnh viễn"))))
                .andExpect(content().string(not(containsString("Set Available"))))
                .andExpect(content().string(not(containsString("Set Maintenance"))));
    }

    @Test
    void adminCanRetireAVehicleThroughTheEditForm() throws Exception {
        mvc.perform(editForm(admin, "retired"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));

        verify(vehicleService).update(eq(5L), any(VehicleFormDTO.class));
    }

    @Test
    void staffCannotRetireAVehicleThroughTheEditForm() throws Exception {
        mvc.perform(editForm(staff, "retired"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));

        verify(vehicleService, never()).update(anyLong(), any());
    }

    @Test
    void staffCanStillEditAVehicleThatIsAlreadyRetired() throws Exception {
        Vehicle retired = new Vehicle();
        retired.setId(5L);
        retired.setStatus(Vehicle.STATUS_RETIRED);
        when(vehicleService.findEntityById(5L)).thenReturn(Optional.of(retired));

        mvc.perform(editForm(staff, "retired"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));

        verify(vehicleService).update(eq(5L), any(VehicleFormDTO.class));
    }

    @Test
    void onlyAdminsAreOfferedTheRetiredStatusInTheEditForm() throws Exception {
        Vehicle working = new Vehicle();
        working.setId(5L);
        working.setStatus(Vehicle.STATUS_AVAILABLE);
        when(vehicleService.findEntityById(5L)).thenReturn(Optional.of(working));

        mvc.perform(get("/admin/vehicles/5/edit").session(admin))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"retired\"")));
        mvc.perform(get("/admin/vehicles/5/edit").session(staff))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("value=\"retired\""))));
    }

    private static MockHttpServletRequestBuilder editForm(MockHttpSession session, String status) {
        return post("/admin/vehicles/5").session(session)
                .param("vehicleTypeId", "3").param("plate", "51A-12345").param("color", "White")
                .param("year", "2023").param("odometer", "1000").param("status", status);
    }

    private void listOf(VehicleDTO... cars) {
        when(vehicleService.searchForAdmin(any())).thenReturn(new PageImpl<>(List.of(cars), PageRequest.of(0, 10), cars.length));
        when(vehicleService.countByStatus()).thenReturn(Map.of("available", 1L));
    }

    private static VehicleDTO car(long id, String status) {
        VehicleDTO dto = new VehicleDTO();
        dto.setId(id);
        dto.setBrand("Toyota");
        dto.setModel("Vios");
        dto.setLicensePlate("51A-" + id);
        dto.setProvince("Ho Chi Minh City");
        dto.setColor("White");
        dto.setYear(2023);
        dto.setOdometer(1000);
        dto.setStatus(status);
        return dto;
    }

    private static MockHttpSession sessionOf(String role) {
        return TestSessions.employee(1L, role, "Test " + role);
    }
}
