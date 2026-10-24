package com.crms.mvc.service;

import com.crms.mvc.dto.ContractTemplateDTO;
import com.crms.mvc.dto.ContractTemplateFormDTO;
import com.crms.mvc.entity.ContractTemplate;
import com.crms.mvc.entity.ContractTemplateStatus;
import com.crms.mvc.entity.Employee;
import com.crms.mvc.repository.ContractTemplateRepository;
import com.crms.mvc.repository.EmployeeRepository;
import com.crms.mvc.service.impl.ContractTemplateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ContractTemplateServiceTest {
    private ContractTemplateRepository templateRepository;
    private EmployeeRepository employeeRepository;
    private ContractTemplateService service;

    @BeforeEach
    void setUp() {
        templateRepository = mock(ContractTemplateRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        service = new ContractTemplateServiceImpl(templateRepository, employeeRepository);
    }

    @Test
    void createDraftCopiesActiveVersionWithNextNumber() {
        ContractTemplate active = template(1L, 3, ContractTemplateStatus.ACTIVE);
        Employee admin = new Employee();
        admin.setId(1L);
        admin.setName("Admin");
        when(templateRepository.existsByStatus(ContractTemplateStatus.DRAFT)).thenReturn(false);
        when(templateRepository.findFirstByStatus(ContractTemplateStatus.ACTIVE)).thenReturn(Optional.of(active));
        when(templateRepository.findMaxVersionNo()).thenReturn(3);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(templateRepository.save(any(ContractTemplate.class))).thenAnswer(call -> call.getArgument(0));

        ContractTemplateDTO draft = service.createDraft(1L);

        assertEquals(4, draft.getVersionNo());
        assertEquals(ContractTemplateStatus.DRAFT, draft.getStatus());
        assertEquals(active.getTermsContent(), draft.getTermsContent());
        assertEquals(0, active.getDepositRate().compareTo(draft.getDepositRate()));
        assertEquals("Admin", draft.getCreatedByName());
    }

    @Test
    void onlyOneDraftAtATime() {
        when(templateRepository.existsByStatus(ContractTemplateStatus.DRAFT)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.createDraft(1L));
        verify(templateRepository, never()).save(any());
    }

    @Test
    void updateDraftConvertsPercentagesToRates() {
        ContractTemplate draft = template(5L, 2, ContractTemplateStatus.DRAFT);
        when(templateRepository.findById(5L)).thenReturn(Optional.of(draft));

        ContractTemplateFormDTO form = new ContractTemplateFormDTO();
        form.setTitle("  Hợp đồng mới  ");
        form.setTermsContent("Điều khoản");
        form.setDepositPercent(new BigDecimal("30"));
        form.setLateFeePercent(new BigDecimal("150"));
        form.setFreeCancellationHours(48);

        service.updateDraft(5L, form);

        assertEquals("Hợp đồng mới", draft.getTitle());
        assertEquals(new BigDecimal("0.3000"), draft.getDepositRate());
        assertEquals(new BigDecimal("1.50"), draft.getLateFeeMultiplier());
        assertEquals(48, draft.getFreeCancellationHours());
    }

    @Test
    void activeVersionCannotBeEdited() {
        ContractTemplate active = template(1L, 1, ContractTemplateStatus.ACTIVE);
        when(templateRepository.findById(1L)).thenReturn(Optional.of(active));

        assertThrows(IllegalStateException.class, () -> service.updateDraft(1L, new ContractTemplateFormDTO()));
        assertThrows(IllegalStateException.class, () -> service.deleteDraft(1L));
    }

    @Test
    void activateArchivesCurrentVersionBeforeActivatingDraft() {
        ContractTemplate current = template(1L, 1, ContractTemplateStatus.ACTIVE);
        ContractTemplate draft = template(2L, 2, ContractTemplateStatus.DRAFT);
        when(templateRepository.findById(2L)).thenReturn(Optional.of(draft));
        when(templateRepository.findFirstByStatus(ContractTemplateStatus.ACTIVE)).thenReturn(Optional.of(current));

        service.activate(2L);

        assertEquals(ContractTemplateStatus.ARCHIVED, current.getStatus());
        assertEquals(ContractTemplateStatus.ACTIVE, draft.getStatus());
        assertNotNull(draft.getActivatedAt());
        // The archive must reach the database first because only one ACTIVE row is allowed.
        InOrder order = inOrder(templateRepository);
        order.verify(templateRepository).saveAndFlush(current);
    }

    private ContractTemplate template(Long id, int version, ContractTemplateStatus status) {
        ContractTemplate template = new ContractTemplate();
        template.setId(id);
        template.setVersionNo(version);
        template.setTitle("Hợp đồng thuê xe ô tô tự lái");
        template.setTermsContent("1. Bàn giao xe đúng hẹn.");
        template.setDepositRate(new BigDecimal("0.5000"));
        template.setLateFeeMultiplier(new BigDecimal("2.00"));
        template.setFreeCancellationHours(72);
        template.setStatus(status);
        template.setCreatedAt(OffsetDateTime.now());
        return template;
    }
}
