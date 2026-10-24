package com.crms.mvc.service.impl;

import com.crms.mvc.dto.ContractTemplateDTO;
import com.crms.mvc.dto.ContractTemplateFormDTO;
import com.crms.mvc.entity.ContractTemplate;
import com.crms.mvc.entity.ContractTemplateStatus;
import com.crms.mvc.entity.Employee;
import com.crms.mvc.repository.ContractTemplateRepository;
import com.crms.mvc.repository.EmployeeRepository;
import com.crms.mvc.service.ContractTemplateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class ContractTemplateServiceImpl implements ContractTemplateService {
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final ContractTemplateRepository contractTemplateRepository;
    private final EmployeeRepository employeeRepository;

    public ContractTemplateServiceImpl(ContractTemplateRepository contractTemplateRepository,
                                       EmployeeRepository employeeRepository) {
        this.contractTemplateRepository = contractTemplateRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ContractTemplateDTO getActiveTemplate() {
        return ContractTemplateDTO.fromEntity(findActive());
    }

    @Override
    @Transactional(readOnly = true)
    public ContractTemplateDTO getTemplate(Long templateId) {
        return ContractTemplateDTO.fromEntity(findById(templateId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractTemplateDTO> getAllTemplates() {
        return contractTemplateRepository.findAllByOrderByVersionNoDesc().stream()
                .map(ContractTemplateDTO::fromEntity)
                .toList();
    }

    @Override
    public ContractTemplateDTO createDraft(Long employeeId) {
        if (contractTemplateRepository.existsByStatus(ContractTemplateStatus.DRAFT)) {
            throw new IllegalStateException("A draft version already exists. Edit or delete it first.");
        }
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NoSuchElementException("Employee not found: " + employeeId));
        ContractTemplate active = findActive();

        ContractTemplate draft = new ContractTemplate();
        draft.setVersionNo(contractTemplateRepository.findMaxVersionNo() + 1);
        draft.setTitle(active.getTitle());
        draft.setTermsContent(active.getTermsContent());
        draft.setDepositRate(active.getDepositRate());
        draft.setLateFeeMultiplier(active.getLateFeeMultiplier());
        draft.setFreeCancellationHours(active.getFreeCancellationHours());
        draft.setStatus(ContractTemplateStatus.DRAFT);
        draft.setCreatedBy(employee);
        draft.setCreatedAt(OffsetDateTime.now());

        return ContractTemplateDTO.fromEntity(contractTemplateRepository.save(draft));
    }

    @Override
    public ContractTemplateDTO updateDraft(Long templateId, ContractTemplateFormDTO form) {
        ContractTemplate draft = findDraft(templateId);
        draft.setTitle(form.getTitle().trim());
        draft.setTermsContent(form.getTermsContent().trim());
        draft.setDepositRate(toRate(form.getDepositPercent(), 4));
        draft.setLateFeeMultiplier(toRate(form.getLateFeePercent(), 2));
        draft.setFreeCancellationHours(form.getFreeCancellationHours());
        return ContractTemplateDTO.fromEntity(draft);
    }

    @Override
    public void activate(Long templateId) {
        ContractTemplate draft = findDraft(templateId);

        contractTemplateRepository.findFirstByStatus(ContractTemplateStatus.ACTIVE).ifPresent(current -> {
            current.setStatus(ContractTemplateStatus.ARCHIVED);
            // Flush first: the database allows only one ACTIVE row at any moment.
            contractTemplateRepository.saveAndFlush(current);
        });

        draft.setStatus(ContractTemplateStatus.ACTIVE);
        draft.setActivatedAt(OffsetDateTime.now());
    }

    @Override
    public void deleteDraft(Long templateId) {
        contractTemplateRepository.delete(findDraft(templateId));
    }

    private ContractTemplate findActive() {
        return contractTemplateRepository.findFirstByStatus(ContractTemplateStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("No active rental terms. Please contact the administrator."));
    }

    private ContractTemplate findById(Long templateId) {
        return contractTemplateRepository.findById(templateId)
                .orElseThrow(() -> new NoSuchElementException("Contract template not found: " + templateId));
    }

    private ContractTemplate findDraft(Long templateId) {
        ContractTemplate template = findById(templateId);
        if (!template.isEditable()) {
            throw new IllegalStateException("Version " + template.getVersionNo() + " is " + template.getStatus()
                    + " and can no longer be changed. Create a new draft instead.");
        }
        return template;
    }

    private BigDecimal toRate(BigDecimal percent, int scale) {
        return percent.divide(ONE_HUNDRED, scale, RoundingMode.HALF_UP);
    }
}
