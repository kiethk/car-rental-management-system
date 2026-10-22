package com.crms.mvc.service;

import com.crms.mvc.dto.LicenseApprovalDTO;
import com.crms.mvc.dto.LicenseSubmitDTO;
import com.crms.mvc.entity.LicenseDocument;

import java.util.List;

public interface LicenseDocumentService {
    LicenseDocument getCustomerLicense(Long customerId);

    boolean submitLicense(Long customerId, LicenseSubmitDTO form);

    List<LicenseDocument> getPendingLicenses();

    LicenseDocument getLicense(Long licenseId);

    boolean reviewLicense(Long licenseId, Long employeeId, LicenseApprovalDTO form);

    boolean isCustomerVerified(Long customerId);
}
