package com.crms.mvc.service.impl;

import com.crms.mvc.dto.LicenseSubmitDTO;
import com.crms.mvc.entity.Customer;
import com.crms.mvc.entity.LicenseDocument;
import com.crms.mvc.repository.CustomerRepository;
import com.crms.mvc.repository.LicenseDocumentRepository;
import com.crms.mvc.service.FileUploadService;
import com.crms.mvc.service.LicenseDocumentService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Service
public class LicenseDocumentServiceImpl implements LicenseDocumentService {
    private final LicenseDocumentRepository licenseDocumentRepository;
    private final CustomerRepository customerRepository;
    private final FileUploadService fileUploadService;

    public LicenseDocumentServiceImpl(LicenseDocumentRepository licenseDocumentRepository,
                                      CustomerRepository customerRepository,
                                      FileUploadService fileUploadService) {
        this.licenseDocumentRepository = licenseDocumentRepository;
        this.customerRepository = customerRepository;
        this.fileUploadService = fileUploadService;
    }

    @Override
    public LicenseDocument getCustomerLicense(Long customerId) {
        return licenseDocumentRepository.findFirstByCustomer_IdOrderBySubmittedAtDesc(customerId)
                .orElse(null);
    }

    @Override
    public boolean submitLicense(Long customerId, LicenseSubmitDTO form) {
        Customer customer = customerRepository.findById(customerId).orElse(null);
        if (customer == null) {
            return false;
        }

        String imageFileName = fileUploadService.saveLicenseImage(form.getLicenseImage());
        licenseDocumentRepository.save(new LicenseDocument(
                customer,
                form.getLicenseNumber().trim(),
                form.getLicenseClass().trim(),
                form.getLicenseExpiry(),
                imageFileName,
                OffsetDateTime.now()
        ));
        return true;
    }

    @Override
    public LicenseDocument getLicense(Long licenseId) {
        return licenseDocumentRepository.findById(licenseId).orElse(null);
    }

    @Override
    public boolean hasValidLicense(Long customerId, LocalDate until) {
        LicenseDocument license = getCustomerLicense(customerId);
        return license != null && !license.getLicenseExpiry().isBefore(until);
    }
}
