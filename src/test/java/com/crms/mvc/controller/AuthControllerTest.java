package com.crms.mvc.controller;

import com.crms.mvc.dto.LoginIdentityDTO;
import com.crms.mvc.service.AuthService;
import com.crms.mvc.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {
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
                .andExpect(content().string(containsString("Email is required")))
                .andExpect(content().string(containsString("Password is required")));
    }

    @Test void wrongPasswordShowsError() throws Exception {
        when(authService.login("a@example.com", "wrong")).thenReturn(null);
        mvc.perform(post("/login").param("email", "a@example.com").param("password", "wrong"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Invalid email or password")));
    }

    @Test void validLoginRedirectsAndStoresSession() throws Exception {
        when(authService.login("a@example.com", "secret"))
                .thenReturn(new LoginIdentityDTO("customer", 1L, "An", null));
        mvc.perform(post("/login").param("email", "a@example.com").param("password", "secret"))
                .andExpect(redirectedUrl("/vehicles"))
                .andExpect(request().sessionAttribute("customerId", 1L))
                .andExpect(request().sessionAttribute("customerName", "An"));
    }

    @Test void logoutRedirectsToLogin() throws Exception {
        mvc.perform(get("/logout")).andExpect(redirectedUrl("/login"));
    }
}
