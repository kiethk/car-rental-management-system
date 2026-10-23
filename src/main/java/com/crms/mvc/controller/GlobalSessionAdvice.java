package com.crms.mvc.controller;

import com.crms.mvc.entity.LicenseDocument;
import com.crms.mvc.service.LicenseDocumentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalSessionAdvice {
    private final ObjectProvider<LicenseDocumentService> licenseDocumentServiceProvider;

    public GlobalSessionAdvice(ObjectProvider<LicenseDocumentService> licenseDocumentServiceProvider) {
        this.licenseDocumentServiceProvider = licenseDocumentServiceProvider;
    }

    @ModelAttribute
    public void populateSessionData(HttpSession session) {
        if (session != null) {
            Long customerId = (Long) session.getAttribute("customerId");
            if (customerId != null && session.getAttribute("licenseStatus") == null) {
                LicenseDocumentService licenseDocumentService = licenseDocumentServiceProvider.getIfAvailable();
                if (licenseDocumentService != null) {
                    LicenseDocument doc = licenseDocumentService.getCustomerLicense(customerId);
                    session.setAttribute("licenseStatus", doc != null ? doc.getStatus() : "not_uploaded");
                }
            }
        }
    }
}
