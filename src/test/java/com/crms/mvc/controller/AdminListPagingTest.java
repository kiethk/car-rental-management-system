package com.crms.mvc.controller;

import com.crms.mvc.dto.AdminVehicleQueryDTO;
import com.crms.mvc.dto.PageQueryDTO;
import com.crms.mvc.dto.VehicleDTO;
import com.crms.mvc.entity.VehicleType;
import com.crms.mvc.service.LicensePlateService;
import com.crms.mvc.service.VehicleService;
import com.crms.mvc.service.VehicleTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AdminVehicleController.class, AdminVehicleTypeController.class})
class AdminListPagingTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private VehicleService vehicleService;
    @MockitoBean private VehicleTypeService vehicleTypeService;
    @MockitoBean private LicensePlateService licensePlateService;

    private MockHttpSession admin;

    @BeforeEach
    void setUp() {
        admin = new MockHttpSession();
        admin.setAttribute("employeeId", 1L);
        admin.setAttribute("employeeRole", "admin");
    }

    @Test
    void carListShowsSearchSortPagerAndStatusCounts() throws Exception {
        List<VehicleDTO> rows = IntStream.rangeClosed(1, 10).mapToObj(this::car).toList();
        when(vehicleService.searchForAdmin(any())).thenReturn(new PageImpl<>(rows, PageRequest.of(1, 10), 37));
        when(vehicleService.countByStatus()).thenReturn(Map.of("available", 30L, "rented", 5L, "maintenance", 2L));

        mvc.perform(get("/admin/vehicles").session(admin)
                        .param("keyword", "toyota").param("sort", "name_desc").param("status", "available").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/vehicles/list"))
                // search box keeps the keyword, sort box keeps the choice
                .andExpect(content().string(containsString("value=\"toyota\"")))
                .andExpect(content().string(containsString("<option value=\"name_desc\" selected=\"selected\"")))
                // status pills show how many cars each has
                .andExpect(content().string(containsString("Available (")))
                .andExpect(content().string(containsString(">37<")))
                // pager: showing 11-20 of 37 and a link to page 3 that keeps the search
                .andExpect(content().string(containsString("11")))
                .andExpect(content().string(containsString("keyword=toyota&amp;sort=name_desc")))
                .andExpect(content().string(containsString("page=3")))
                // action forms send the user back to this very page
                .andExpect(content().string(containsString("name=\"returnTo\"")))
                .andExpect(content().string(containsString("page=2")));

        ArgumentCaptor<AdminVehicleQueryDTO> captor = ArgumentCaptor.forClass(AdminVehicleQueryDTO.class);
        verify(vehicleService).searchForAdmin(captor.capture());
        assertEquals("toyota", captor.getValue().getKeyword());
        assertEquals("name_desc", captor.getValue().getSort());
        assertEquals("available", captor.getValue().getStatus());
        assertEquals(1, captor.getValue().getPageIndex());
    }

    @Test
    void carListWithNoMatchOffersToShowAllCars() throws Exception {
        when(vehicleService.searchForAdmin(any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        when(vehicleService.countByStatus()).thenReturn(Map.of());

        mvc.perform(get("/admin/vehicles").session(admin).param("keyword", "zzz"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No car matches your search.")))
                .andExpect(content().string(containsString("Show all cars")))
                .andExpect(content().string(not(containsString("pagination"))));
    }

    @Test
    void statusSwitchReturnsToTheSameListPage() throws Exception {
        mvc.perform(post("/admin/vehicles/5/status").session(admin)
                        .param("status", "maintenance")
                        .param("returnTo", "/admin/vehicles?keyword=toyota&sort=name_asc&size=10&status=all&page=3"))
                .andExpect(redirectedUrl("/admin/vehicles?keyword=toyota&sort=name_asc&size=10&status=all&page=3"));
    }

    @Test
    void returnToCanNeverLeaveTheAdminList() throws Exception {
        for (String evil : List.of("https://evil.example/phish", "//evil.example", "/admin/vehicles//evil.example", "/other")) {
            mvc.perform(post("/admin/vehicles/5/delete").session(admin).param("returnTo", evil))
                    .andExpect(redirectedUrl("/admin/vehicles"));
        }
    }

    @Test
    void modelListShowsSearchSortAndPager() throws Exception {
        List<VehicleType> types = List.of(type(1L, "Vios", "Toyota"), type(2L, "Accent", "Hyundai"));
        Page<VehicleType> page = new PageImpl<>(types, PageRequest.of(0, 10), 2);
        when(vehicleTypeService.searchForAdmin(any())).thenReturn(page);
        when(vehicleTypeService.countVehiclesOfType(any())).thenReturn(3L);

        mvc.perform(get("/admin/vehicle-types").session(admin).param("keyword", "to").param("sort", "name_desc"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/vehicle-types/list"))
                .andExpect(content().string(containsString("value=\"to\"")))
                .andExpect(content().string(containsString("<option value=\"name_desc\" selected=\"selected\"")))
                .andExpect(content().string(containsString("Toyota Vios")))
                .andExpect(content().string(containsString("name=\"returnTo\"")));

        ArgumentCaptor<PageQueryDTO> captor = ArgumentCaptor.forClass(PageQueryDTO.class);
        verify(vehicleTypeService).searchForAdmin(captor.capture());
        assertEquals("to", captor.getValue().getKeyword());
        assertEquals("name_desc", captor.getValue().getSort());
    }

    @Test
    void modelDeleteReturnsToTheSameListPage() throws Exception {
        mvc.perform(post("/admin/vehicle-types/4/delete").session(admin)
                        .param("returnTo", "/admin/vehicle-types?keyword=kia&sort=name_asc&size=10&page=2"))
                .andExpect(redirectedUrl("/admin/vehicle-types?keyword=kia&sort=name_asc&size=10&page=2"));

        mvc.perform(post("/admin/vehicle-types/4/delete").session(admin).param("returnTo", "https://evil.example"))
                .andExpect(redirectedUrl("/admin/vehicle-types"));
    }

    private VehicleDTO car(int id) {
        VehicleDTO dto = new VehicleDTO();
        dto.setId((long) id);
        dto.setBrand("Toyota");
        dto.setModel("Vios");
        dto.setLicensePlate("51A-" + id);
        dto.setProvince("Ho Chi Minh City");
        dto.setColor("White");
        dto.setYear(2023);
        dto.setOdometer(1000);
        dto.setStatus("available");
        return dto;
    }

    private VehicleType type(Long id, String name, String brand) {
        VehicleType type = new VehicleType();
        type.setId(id);
        type.setName(name);
        type.setBrand(brand);
        type.setCategory("sedan");
        type.setSeats(5);
        type.setTransmission("automatic");
        type.setFuelType("gasoline");
        type.setPricePerHour(new BigDecimal("150000"));
        type.setDescription("desc");
        return type;
    }
}
