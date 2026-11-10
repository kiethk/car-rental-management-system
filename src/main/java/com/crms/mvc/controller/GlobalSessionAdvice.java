package com.crms.mvc.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalSessionAdvice {
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
}
