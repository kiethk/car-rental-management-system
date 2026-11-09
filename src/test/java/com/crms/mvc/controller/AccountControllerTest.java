package com.crms.mvc.controller;

import com.crms.mvc.dto.AccountProfileDTO;
import com.crms.mvc.dto.SessionAccount;
import com.crms.mvc.service.AccountService;
import com.crms.mvc.support.PassThroughSessionAccountService;
import com.crms.mvc.support.TestSessions;
import com.crms.mvc.util.CurrentAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(PassThroughSessionAccountService.class)
@WebMvcTest(AccountController.class)
class AccountControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private AccountService accountService;

    @BeforeEach
    void setUp() {
        AccountProfileDTO profile = new AccountProfileDTO();
        profile.setName("Nguyen Van An");
        profile.setEmail("an@example.com");
        profile.setPhone("0901234567");
        when(accountService.getProfile(any())).thenReturn(profile);
    }

    @Test
    void customerSeesSingleAccountForm() throws Exception {
        mvc.perform(get("/profile").session(TestSessions.customer(1L)))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/index"))
                .andExpect(content().string(containsString("an@example.com")))
                .andExpect(content().string(containsString("name=\"confirmPassword\"")));
    }

    @Test
    void phoneOnlyUpdateDoesNotTouchPassword() throws Exception {
        when(accountService.update(any(), any())).thenReturn(customerAccount(TestSessions.STAMP));

        mvc.perform(post("/profile").session(TestSessions.customer(1L)).param("phone", "0912345678"))
                .andExpect(redirectedUrl("/profile?updated"));

        ArgumentCaptor<AccountProfileDTO> form = ArgumentCaptor.forClass(AccountProfileDTO.class);
        verify(accountService).update(any(), form.capture());
        assertThat(form.getValue().getPhone()).isEqualTo("0912345678");
        assertThat(form.getValue().isPasswordChangeRequested()).isFalse();
    }

    @Test
    void wrongCurrentPasswordIsShownOnTheField() throws Exception {
        when(accountService.update(any(), any()))
                .thenThrow(new IllegalArgumentException("Mật khẩu hiện tại không đúng"));

        mvc.perform(post("/profile").session(TestSessions.customer(1L))
                        .param("phone", "0901234567")
                        .param("currentPassword", "wrongPass1")
                        .param("newPassword", "newSecret123")
                        .param("confirmPassword", "newSecret123"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/index"))
                .andExpect(model().attributeHasFieldErrors("profileForm", "currentPassword"))
                .andExpect(content().string(containsString("Mật khẩu hiện tại không đúng")));
    }

    @Test
    void mismatchedConfirmationIsRejectedBeforeSaving() throws Exception {
        mvc.perform(post("/profile").session(TestSessions.customer(1L))
                        .param("phone", "0901234567")
                        .param("currentPassword", "oldSecret123")
                        .param("newPassword", "newSecret123")
                        .param("confirmPassword", "different123"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mật khẩu xác nhận không khớp")));

        verify(accountService, never()).update(any(), any());
    }

    @Test
    void readOnlyAndPrivilegedFieldsCannotBeBound() throws Exception {
        when(accountService.update(any(), any())).thenReturn(customerAccount(TestSessions.STAMP));

        mvc.perform(post("/profile").session(TestSessions.customer(1L))
                        .param("phone", "0901234567")
                        .param("name", "Hacker")
                        .param("email", "hacker@example.com")
                        .param("role", "admin")
                        .param("id", "99"))
                .andExpect(redirectedUrl("/profile?updated"));

        ArgumentCaptor<AccountProfileDTO> form = ArgumentCaptor.forClass(AccountProfileDTO.class);
        verify(accountService).update(any(), form.capture());
        assertThat(form.getValue().getName()).isNull();
        assertThat(form.getValue().getEmail()).isNull();
    }

    @Test
    void passwordChangeRotatesSessionIdAndStoresNewStamp() throws Exception {
        SessionAccount updated = customerAccount("new-stamp");
        when(accountService.update(any(), any())).thenReturn(updated);
        MockHttpSession session = TestSessions.customer(1L);
        String oldId = session.getId();

        mvc.perform(post("/profile").session(session)
                        .param("phone", "0901234567")
                        .param("currentPassword", "oldSecret123")
                        .param("newPassword", "newSecret123")
                        .param("confirmPassword", "newSecret123"))
                .andExpect(redirectedUrl("/profile?updated"));

        assertThat(session.getId()).isNotEqualTo(oldId);
        assertThat(session.getAttribute(CurrentAccount.SESSION_KEY)).isSameAs(updated);
    }

    @Test
    void anonymousUserIsSentToLogin() throws Exception {
        mvc.perform(post("/profile").param("phone", "0901234567"))
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void employeeUpdatesOwnAccount() throws Exception {
        when(accountService.update(any(), any())).thenReturn(new SessionAccount(2L, SessionAccount.EMPLOYEE,
                "Staff", "staff@example.com", "staff", TestSessions.STAMP));

        mvc.perform(post("/admin/profile").session(TestSessions.staff(2L)).param("phone", "0901234567"))
                .andExpect(redirectedUrl("/admin/profile?updated"));
    }

    private SessionAccount customerAccount(String stamp) {
        return new SessionAccount(1L, SessionAccount.CUSTOMER, "Nguyen Van An", "an@example.com", null, stamp);
    }
}
