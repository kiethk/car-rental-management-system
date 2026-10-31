package com.crms.mvc.controller;

import com.crms.mvc.entity.LicenseDocument;
import com.crms.mvc.service.LicenseDocumentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalSessionAdvice {
    private final LicenseDocumentService licenseDocumentService;

    public GlobalSessionAdvice(@Autowired(required = false) LicenseDocumentService licenseDocumentService) {
        this.licenseDocumentService = licenseDocumentService;
    }

    @ModelAttribute("isAdminPage")
    public boolean isAdminPage(HttpServletRequest request) {
        if (request == null) return false;
        String uri = request.getRequestURI();
        if (uri == null) return false;
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        return uri.startsWith("/admin");
    }

    @ModelAttribute
    public void populateSessionData(HttpSession session) {
        if (session != null && licenseDocumentService != null) {
            Long customerId = (Long) session.getAttribute("customerId");
            if (customerId != null && session.getAttribute("licenseStatus") == null) {
                LicenseDocument doc = licenseDocumentService.getCustomerLicense(customerId);
                session.setAttribute("licenseStatus", doc != null ? doc.getStatus() : "not_uploaded");
            }
        }
    }
}
