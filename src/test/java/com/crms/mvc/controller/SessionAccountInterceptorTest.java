package com.crms.mvc.controller;

import com.crms.mvc.dto.AccountProfileDTO;
import com.crms.mvc.dto.SessionAccount;
import com.crms.mvc.service.AccountService;
import com.crms.mvc.service.SessionAccountService;
import com.crms.mvc.support.TestSessions;
import com.crms.mvc.util.CurrentAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
class SessionAccountInterceptorTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private SessionAccountService sessionAccountService;
    @MockitoBean private AccountService accountService;

    @BeforeEach
    void setUp() {
        AccountProfileDTO profile = new AccountProfileDTO();
        profile.setName("Someone");
        profile.setEmail("someone@example.com");
        profile.setPhone("0901234567");
        when(accountService.getProfile(any())).thenReturn(profile);
    }

    @Test
    void deletedOrChangedEmployeeIsLoggedOut() throws Exception {
        when(sessionAccountService.refresh(any())).thenReturn(Optional.empty());
        MockHttpSession session = TestSessions.staff(2L);

        // The session is gone, so /admin sends them to log in again rather than a bare 403.
        mvc.perform(get("/admin/profile").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void staleCustomerSessionIsSentToLogin() throws Exception {
        when(sessionAccountService.refresh(any())).thenReturn(Optional.empty());
        MockHttpSession session = TestSessions.customer(1L);

        mvc.perform(get("/profile").session(session))
                .andExpect(redirectedUrl("/login"));

        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void tamperedSessionValueIsIgnored() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(CurrentAccount.SESSION_KEY, "admin");

        mvc.perform(get("/profile").session(session))
                .andExpect(redirectedUrl("/login"));

        assertThat(session.getAttribute(CurrentAccount.SESSION_KEY)).isNull();
    }

    @Test
    void roleIsTakenFromDatabaseNotFromSession() throws Exception {
        SessionAccount demoted = new SessionAccount(1L, SessionAccount.CUSTOMER, "Former Admin",
                "x@example.com", null, TestSessions.STAMP);
        when(sessionAccountService.refresh(any())).thenReturn(Optional.of(demoted));
        MockHttpSession session = TestSessions.admin(1L);

        mvc.perform(get("/admin/profile").session(session))
                .andExpect(status().isForbidden());

        assertThat(session.getAttribute(CurrentAccount.SESSION_KEY)).isSameAs(demoted);
    }

    @Test
    void databaseOutageKeepsSessionAndReturns503() throws Exception {
        when(sessionAccountService.refresh(any())).thenThrow(new DataAccessResourceFailureException("down"));
        MockHttpSession session = TestSessions.customer(1L);

        mvc.perform(get("/profile").session(session))
                .andExpect(status().isServiceUnavailable());

        assertThat(session.isInvalid()).isFalse();
        assertThat(session.getAttribute(CurrentAccount.SESSION_KEY)).isNotNull();
    }

    @Test
    void customerCannotOpenAdminPages() throws Exception {
        when(sessionAccountService.refresh(any())).thenAnswer(inv -> Optional.of(inv.getArgument(0)));

        mvc.perform(get("/admin/profile").session(TestSessions.customer(1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void employeeOnCustomerProfileIsSentToAdminProfile() throws Exception {
        when(sessionAccountService.refresh(any())).thenAnswer(inv -> Optional.of(inv.getArgument(0)));

        mvc.perform(get("/profile").session(TestSessions.staff(2L)))
                .andExpect(redirectedUrl("/admin/profile"));
    }
}
