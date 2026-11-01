package com.crms.mvc.service;

import com.crms.mvc.dto.LicenseSubmitDTO;
import com.crms.mvc.entity.LicenseDocument;

import java.time.LocalDate;

public interface LicenseDocumentService {
    LicenseDocument getCustomerLicense(Long customerId);

    boolean submitLicense(Long customerId, LicenseSubmitDTO form);

    LicenseDocument getLicense(Long licenseId);

    boolean hasValidLicense(Long customerId, LocalDate until);
}
