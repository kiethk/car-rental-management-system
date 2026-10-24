package com.crms.mvc.controller;

import com.crms.mvc.dto.ContractTemplateDTO;
import com.crms.mvc.entity.ContractTemplateStatus;
import com.crms.mvc.service.ContractTemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminContractTemplateController.class)
class AdminContractTemplateControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private ContractTemplateService contractTemplateService;

    @Test
    void listShowsVersionsWithPlainPercentages() throws Exception {
        when(contractTemplateService.getAllTemplates())
                .thenReturn(List.of(template(2L, 2, ContractTemplateStatus.DRAFT), template(1L, 1, ContractTemplateStatus.ACTIVE)));

        mvc.perform(get("/admin/contract-templates").session(session("admin")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("v2")))
                .andExpect(content().string(containsString("50%")))
                .andExpect(content().string(containsString("200% of rate")))
                .andExpect(content().string(not(containsString("E+"))))
                .andExpect(content().string(containsString("New draft from active")));
    }

    @Test
    void draftDetailShowsEditorForAdmin() throws Exception {
        when(contractTemplateService.getTemplate(2L)).thenReturn(template(2L, 2, ContractTemplateStatus.DRAFT));

        mvc.perform(get("/admin/contract-templates/2").session(session("admin")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"depositPercent\"")))
                .andExpect(content().string(containsString("value=\"50\"")))
                .andExpect(content().string(containsString("Activate saved draft")));
    }

    @Test
    void activeVersionIsReadOnly() throws Exception {
        when(contractTemplateService.getTemplate(1L)).thenReturn(template(1L, 1, ContractTemplateStatus.ACTIVE));

        mvc.perform(get("/admin/contract-templates/1").session(session("admin")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("name=\"depositPercent\""))))
                .andExpect(content().string(containsString("can no longer be changed")));
    }

    @Test
    void staffCannotActivate() throws Exception {
        mvc.perform(post("/admin/contract-templates/2/activate").session(session("staff")))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "Only administrators can change the rental terms."));

        verify(contractTemplateService, never()).activate(anyLong());
    }

    @Test
    void invalidDraftShowsFieldErrors() throws Exception {
        when(contractTemplateService.getTemplate(2L)).thenReturn(template(2L, 2, ContractTemplateStatus.DRAFT));

        mvc.perform(post("/admin/contract-templates/2").session(session("admin"))
                        .param("title", "Hợp đồng")
                        .param("termsContent", "Nội dung")
                        .param("depositPercent", "150")
                        .param("lateFeePercent", "200")
                        .param("freeCancellationHours", "72"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/contract-templates/detail"))
                .andExpect(content().string(containsString("Deposit cannot exceed 100%")));

        verify(contractTemplateService, never()).updateDraft(anyLong(), any());
    }

    private MockHttpSession session(String role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("employeeId", 1L);
        session.setAttribute("employeeName", "Admin Manager");
        session.setAttribute("employeeRole", role);
        return session;
    }

    private ContractTemplateDTO template(Long id, int version, ContractTemplateStatus status) {
        ContractTemplateDTO dto = new ContractTemplateDTO();
        dto.setId(id);
        dto.setVersionNo(version);
        dto.setTitle("Hợp đồng thuê xe ô tô tự lái");
        dto.setTermsContent("1. Bàn giao xe đúng hẹn.");
        dto.setDepositRate(new BigDecimal("0.5000"));
        dto.setLateFeeMultiplier(new BigDecimal("2.00"));
        dto.setFreeCancellationHours(72);
        dto.setStatus(status);
        dto.setCreatedAt(OffsetDateTime.now());
        dto.setActivatedAt(status == ContractTemplateStatus.ACTIVE ? OffsetDateTime.now() : null);
        return dto;
    }
}
