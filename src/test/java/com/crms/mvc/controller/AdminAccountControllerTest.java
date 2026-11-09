package com.crms.mvc.controller;

import com.crms.mvc.dto.AccountQueryDTO;
import com.crms.mvc.dto.AccountSummaryDTO;
import com.crms.mvc.dto.SessionAccount;
import com.crms.mvc.service.AccountManagementService;
import com.crms.mvc.support.PassThroughSessionAccountService;
import com.crms.mvc.support.TestSessions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(PassThroughSessionAccountService.class)
@WebMvcTest(AdminAccountController.class)
class AdminAccountControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private AccountManagementService accountManagementService;

    @Test
    void adminSeesEmployeeListWithoutLockButtonOnOwnAccount() throws Exception {
        when(accountManagementService.search(any(AccountQueryDTO.class))).thenReturn(new PageImpl<>(List.of(
                new AccountSummaryDTO(1L, SessionAccount.EMPLOYEE, "Admin Manager", "admin@crms.test", "0900000001", "admin", true),
                new AccountSummaryDTO(2L, SessionAccount.EMPLOYEE, "Locked Staff", "staff@crms.test", "0900000002", "staff", false)),
                PageRequest.of(0, 10), 2));

        mvc.perform(get("/admin/accounts").param("type", "employee").session(TestSessions.admin(1L)))
                .andExpect(status().isOk())
                .andExpect(view().name("screens/admin/accounts/list/index"))
                .andExpect(content().string(containsString("Quản lý tài khoản")))
                .andExpect(content().string(containsString("Locked Staff")))
                .andExpect(content().string(containsString("Tài khoản của bạn")))
                .andExpect(content().string(containsString("Kích hoạt")))
                .andExpect(content().string(not(containsString("/admin/accounts/employee/1/status"))));
    }

    @Test
    void staffIsForbiddenEverywhere() throws Exception {
        mvc.perform(get("/admin/accounts").session(TestSessions.staff(2L))).andExpect(status().isForbidden());
        mvc.perform(get("/admin/accounts/employees/new").session(TestSessions.staff(2L))).andExpect(status().isForbidden());
        mvc.perform(post("/admin/accounts/customer/5/status").param("active", "false").session(TestSessions.staff(2L)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/accounts/employees").session(TestSessions.staff(2L))).andExpect(status().isForbidden());

        verify(accountManagementService, never()).setActive(any(), any(), eq(false), any());
    }

    @Test
    void adminLocksCustomer() throws Exception {
        mvc.perform(post("/admin/accounts/customer/5/status").param("active", "false").session(TestSessions.admin(1L)))
                .andExpect(redirectedUrl("/admin/accounts?type=customer"));

        verify(accountManagementService).setActive(eq("customer"), eq(5L), eq(false), any(SessionAccount.class));
    }

    @Test
    void invalidEmployeeFormShowsErrors() throws Exception {
        mvc.perform(post("/admin/accounts/employees").session(TestSessions.admin(1L))
                        .param("name", "").param("phone", "abc").param("email", "bad")
                        .param("password", "short").param("role", "root"))
                .andExpect(status().isOk())
                .andExpect(view().name("screens/admin/accounts/form/index"))
                .andExpect(content().string(containsString("Vui lòng nhập họ tên")))
                .andExpect(content().string(containsString("Vai trò phải là nhân viên hoặc quản trị viên")));
    }

    @Test
    void employeeFormRendersAndDuplicateEmailIsShown() throws Exception {
        mvc.perform(get("/admin/accounts/employees/new").session(TestSessions.admin(1L)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mật khẩu tạm thời")));

        org.mockito.Mockito.doThrow(new IllegalArgumentException("Email is already in use"))
                .when(accountManagementService).createEmployee(any());
        mvc.perform(post("/admin/accounts/employees").session(TestSessions.admin(1L))
                        .param("name", "New Staff").param("phone", "0900000000").param("email", "new@crms.test")
                        .param("password", "temporary1").param("role", "staff"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Email này đã được sử dụng")));
    }
}
