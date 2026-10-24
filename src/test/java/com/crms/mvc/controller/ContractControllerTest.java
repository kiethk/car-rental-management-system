package com.crms.mvc.controller;

import com.crms.mvc.dto.ContractDTO;
import com.crms.mvc.service.ContractService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ContractController.class)
class ContractControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private ContractService contractService;

    @Test
    void ownerDownloadsPdfInline() throws Exception {
        when(contractService.loadContractPdfForCustomer(42L, 1L)).thenReturn(new ByteArrayResource("%PDF-1.7".getBytes()));

        mvc.perform(get("/bookings/42/contract").session(customer(1L)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"contract-booking-42.pdf\""));
    }

    @Test
    void otherCustomerIsRejected() throws Exception {
        when(contractService.loadContractPdfForCustomer(42L, 2L))
                .thenThrow(new IllegalStateException("Unauthorized access to contract"));

        mvc.perform(get("/bookings/42/contract").session(customer(2L)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anonymousUserIsSentToLogin() throws Exception {
        mvc.perform(get("/bookings/42/contract"))
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void staffCanRetryIssuing() throws Exception {
        ContractDTO emailed = new ContractDTO();
        emailed.setEmailedAt(OffsetDateTime.now());
        when(contractService.findByBookingId(42L)).thenReturn(Optional.of(emailed));

        mvc.perform(post("/admin/bookings/42/contract/issue").session(staff()))
                .andExpect(redirectedUrl("/admin/bookings/42"))
                .andExpect(flash().attribute("success", "Contract PDF issued and emailed to the customer."));
        verify(contractService).issueContract(42L);
    }

    @Test
    void staffIsToldWhenEmailIsNotConfigured() throws Exception {
        when(contractService.findByBookingId(42L)).thenReturn(Optional.of(new ContractDTO()));

        mvc.perform(post("/admin/bookings/42/contract/issue").session(staff()))
                .andExpect(redirectedUrl("/admin/bookings/42"))
                .andExpect(flash().attribute("error", org.hamcrest.Matchers.containsString("email was not sent")));
    }

    private MockHttpSession staff() {
        MockHttpSession staff = new MockHttpSession();
        staff.setAttribute("employeeId", 2L);
        staff.setAttribute("employeeRole", "staff");
        return staff;
    }

    private MockHttpSession customer(Long customerId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("customerId", customerId);
        session.setAttribute("licenseStatus", "approved");
        return session;
    }
}
