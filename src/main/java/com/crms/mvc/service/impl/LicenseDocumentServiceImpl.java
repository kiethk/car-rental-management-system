package com.crms.mvc.service.impl;

import com.crms.mvc.dto.LicenseApprovalDTO;
import com.crms.mvc.dto.LicenseSubmitDTO;
import com.crms.mvc.entity.Customer;
import com.crms.mvc.entity.Employee;
import com.crms.mvc.entity.LicenseDocument;
import com.crms.mvc.repository.CustomerRepository;
import com.crms.mvc.repository.EmployeeRepository;
import com.crms.mvc.repository.LicenseDocumentRepository;
import com.crms.mvc.service.FileUploadService;
import com.crms.mvc.service.LicenseDocumentService;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class LicenseDocumentServiceImpl implements LicenseDocumentService {
    private final LicenseDocumentRepository licenseDocumentRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final FileUploadService fileUploadService;

    public LicenseDocumentServiceImpl(LicenseDocumentRepository licenseDocumentRepository,
                                      CustomerRepository customerRepository,
                                      EmployeeRepository employeeRepository,
                                      FileUploadService fileUploadService) {
        this.licenseDocumentRepository = licenseDocumentRepository;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.fileUploadService = fileUploadService;
    }

    @Override
    public LicenseDocument getCustomerLicense(Long customerId) {
        List<LicenseDocument> documents =
                licenseDocumentRepository.findByCustomer_IdOrderBySubmittedAtDesc(customerId);
        if (documents.isEmpty()) {
            return null;
        }
        return documents.get(0);
    }

    @Override
    public boolean submitLicense(Long customerId, LicenseSubmitDTO form) {
        if (licenseDocumentRepository.existsByCustomer_IdAndStatus(customerId, "pending")
                || isCustomerVerified(customerId)) {
            return false;
        }

        Customer customer = customerRepository.findById(customerId).orElse(null);
        if (customer == null) {
            return false;
        }

        String imageFileName = fileUploadService.saveLicenseImage(form.getLicenseImage());
        LicenseDocument document = new LicenseDocument(
                customer,
                form.getLicenseNumber().trim(),
                form.getLicenseClass().trim(),
                form.getLicenseExpiry(),
                imageFileName,
                "pending",
                OffsetDateTime.now()
        );
        licenseDocumentRepository.save(document);
        return true;
    }

    @Override
    public List<LicenseDocument> getPendingLicenses() {
        return licenseDocumentRepository.findByStatusOrderBySubmittedAtAsc("pending");
    }

    @Override
    public LicenseDocument getLicense(Long licenseId) {
        return licenseDocumentRepository.findById(licenseId).orElse(null);
    }

    @Override
    public boolean reviewLicense(Long licenseId, Long employeeId, LicenseApprovalDTO form) {
        LicenseDocument document = licenseDocumentRepository.findById(licenseId).orElse(null);
        Employee employee = employeeRepository.findById(employeeId).orElse(null);

        if (document == null || employee == null || !"pending".equals(document.getStatus())) {
            return false;
        }

        if ("rejected".equals(form.getDecision())
                && (form.getRejectionReason() == null || form.getRejectionReason().isBlank())) {
            return false;
        }

        document.setStatus(form.getDecision());
        document.setReviewedBy(employee);
        document.setReviewedAt(OffsetDateTime.now());
        document.setRejectionReason(
                "rejected".equals(form.getDecision()) ? form.getRejectionReason().trim() : null
        );
        licenseDocumentRepository.save(document);
        return true;
    }

    @Override
    public boolean isCustomerVerified(Long customerId) {
        List<LicenseDocument> documents =
                licenseDocumentRepository.findByCustomer_IdOrderBySubmittedAtDesc(customerId);

        for (LicenseDocument document : documents) {
            if ("approved".equals(document.getStatus())) {
                return true;
            }
        }

        return false;
    }
}
