package com.crms.mvc.service;

import com.crms.mvc.dto.ContractTemplateDTO;
import com.crms.mvc.dto.ContractTemplateFormDTO;

import java.util.List;

public interface ContractTemplateService {
    /** The version every new booking must accept. */
    ContractTemplateDTO getActiveTemplate();

    ContractTemplateDTO getTemplate(Long templateId);

    List<ContractTemplateDTO> getAllTemplates();

    /** Copies the active version into a new DRAFT; only one draft may exist at a time. */
    ContractTemplateDTO createDraft(Long employeeId);

    ContractTemplateDTO updateDraft(Long templateId, ContractTemplateFormDTO form);

    /** Archives the current ACTIVE version and activates the given DRAFT. */
    void activate(Long templateId);

    void deleteDraft(Long templateId);
}
