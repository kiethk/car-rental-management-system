package com.crms.mvc.controller;

import com.crms.mvc.service.LicenseDocumentService;
import com.crms.mvc.util.CurrentAccount;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalSessionAdvice {
    private final LicenseDocumentService licenseDocumentService;

    public GlobalSessionAdvice(@Autowired(required = false) LicenseDocumentService licenseDocumentService) {
        this.licenseDocumentService = licenseDocumentService;
    }

    @ModelAttribute
    public void populateLicenseFlag(HttpServletRequest request, Model model) {
        if (licenseDocumentService == null) {
            return;
        }
        CurrentAccount.customerId(request.getSession(false)).ifPresent(customerId ->
                model.addAttribute("hasLicense",
                        licenseDocumentService.getCustomerLicense(customerId) != null));
    }
}
