package com.crms.mvc.controller;

import com.crms.mvc.dto.SessionAccount;
import com.crms.mvc.support.PassThroughSessionAccountService;
import com.crms.mvc.util.CurrentAccount;
import com.crms.mvc.service.PromotionService;
import com.crms.mvc.service.ReviewService;
import org.springframework.context.annotation.Import;
import com.crms.mvc.service.VehicleTypeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(PassThroughSessionAccountService.class)
@WebMvcTest(HomeController.class)
class HeaderNavigationTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private VehicleTypeService vehicleTypeService;
    @MockitoBean private PromotionService promotionService;
    @MockitoBean private ReviewService reviewService;

    @Test
    void whenEmployeeViewsCustomerHome_showsBackToAdminAndHidesViewCustomerPage() throws Exception {
        when(vehicleTypeService.findAll()).thenReturn(List.of());

        MockHttpSession staffSession = new MockHttpSession();
        staffSession.setAttribute(CurrentAccount.SESSION_KEY,
                new SessionAccount(2L, SessionAccount.EMPLOYEE, "Staff Member", "staff@crms.test", "staff", null));

        mvc.perform(get("/").session(staffSession))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(content().string(containsString("Về trang Quản trị")))
                .andExpect(content().string(containsString("/admin/dashboard")))
                .andExpect(content().string(containsString("Xem trước (Nhân viên)")))
                .andExpect(content().string(not(containsString("Xem trang khách"))))
                .andExpect(content().string(containsString("Danh sách xe")));
    }

    @Test
    void whenGuestViewsCustomerHome_showsLoginAndRegister() throws Exception {
        when(vehicleTypeService.findAll()).thenReturn(List.of());

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(content().string(containsString("Đăng nhập")))
                .andExpect(content().string(containsString("Đăng ký")))
                .andExpect(content().string(not(containsString("Về trang Quản trị"))))
                .andExpect(content().string(not(containsString("Xem trang khách"))));
    }
}
