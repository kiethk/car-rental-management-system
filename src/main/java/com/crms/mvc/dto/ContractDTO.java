package com.crms.mvc.dto;

import com.crms.mvc.entity.Contract;
import com.crms.mvc.util.DateTimeUtils;

import java.io.Serializable;
import java.time.OffsetDateTime;

public class ContractDTO implements Serializable {
    private Long id;
    private String contractNumber;
    private OffsetDateTime acceptedAt;
    private String acceptedIp;
    private boolean issued;
    private OffsetDateTime issuedAt;
    private OffsetDateTime emailedAt;
    private ContractTemplateDTO terms;

    public static ContractDTO fromEntity(Contract contract) {
        ContractDTO dto = new ContractDTO();
        dto.setId(contract.getId());
        dto.setContractNumber(contract.getContractNumber());
        dto.setAcceptedAt(DateTimeUtils.toVietnamTime(contract.getAcceptedAt()));
        dto.setAcceptedIp(contract.getAcceptedIp());
        dto.setIssued(contract.isIssued());
        dto.setIssuedAt(DateTimeUtils.toVietnamTime(contract.getIssuedAt()));
        dto.setEmailedAt(DateTimeUtils.toVietnamTime(contract.getEmailedAt()));
        dto.setTerms(ContractTemplateDTO.fromEntity(contract.getTemplate()));
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContractNumber() { return contractNumber; }
    public void setContractNumber(String contractNumber) { this.contractNumber = contractNumber; }

    public OffsetDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(OffsetDateTime acceptedAt) { this.acceptedAt = acceptedAt; }

    public String getAcceptedIp() { return acceptedIp; }
    public void setAcceptedIp(String acceptedIp) { this.acceptedIp = acceptedIp; }

    public boolean isIssued() { return issued; }
    public void setIssued(boolean issued) { this.issued = issued; }

    public OffsetDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(OffsetDateTime issuedAt) { this.issuedAt = issuedAt; }

    public OffsetDateTime getEmailedAt() { return emailedAt; }
    public void setEmailedAt(OffsetDateTime emailedAt) { this.emailedAt = emailedAt; }

    public ContractTemplateDTO getTerms() { return terms; }
    public void setTerms(ContractTemplateDTO terms) { this.terms = terms; }
}
