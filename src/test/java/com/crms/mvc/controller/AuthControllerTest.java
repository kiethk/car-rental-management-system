package com.crms.mvc.controller;

import com.crms.mvc.service.LicenseDocumentService;
import org.springframework.context.annotation.Import;
import com.crms.mvc.support.PassThroughSessionAccountService;
import com.crms.mvc.dto.LoginIdentityDTO;
import com.crms.mvc.service.AuthService;
import com.crms.mvc.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(PassThroughSessionAccountService.class)
@WebMvcTest(AuthController.class)
class AuthControllerTest {
    @MockitoBean private LicenseDocumentService licenseDocumentService; // the global advice reads it on every page
    @Autowired private MockMvc mvc;
    @MockitoBean private AuthService authService;
    @MockitoBean private CustomerService customerService;

    @Test void showsLoginForm() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(view().name("auth/login"))
                .andExpect(content().string(containsString("name=\"email\"")));
    }

    @Test void blankFieldsShowValidationErrors() throws Exception {
        mvc.perform(post("/login").param("email", "").param("password", ""))
                .andExpect(status().isOk()).andExpect(view().name("auth/login"))
                .andExpect(content().string(containsString("Vui lòng nhập email")))
                .andExpect(content().string(containsString("Vui lòng nhập mật khẩu")));
    }

    @Test void wrongPasswordShowsError() throws Exception {
        when(authService.login("a@example.com", "wrong")).thenReturn(null);
        mvc.perform(post("/login").param("email", "a@example.com").param("password", "wrong"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Email hoặc mật khẩu không đúng")));
    }

    @Test void validLoginRedirectsAndStoresSession() throws Exception {
        when(authService.login("a@example.com", "secret"))
                .thenReturn(new LoginIdentityDTO("customer", 1L, "An", null));
        mvc.perform(post("/login").param("email", "a@example.com").param("password", "secret"))
                .andExpect(redirectedUrl("/vehicles"))
                .andExpect(request().sessionAttribute("account", allOf(
                        hasProperty("id", is(1L)),
                        hasProperty("name", is("An")),
                        hasProperty("customer", is(true)))));
    }

    @Test void logoutRedirectsToLogin() throws Exception {
        mvc.perform(get("/logout")).andExpect(redirectedUrl("/login"));
    }
}
