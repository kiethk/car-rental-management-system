package com.crms.mvc.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RoleInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("employeeId") == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        String role = (String) session.getAttribute("employeeRole");
        if ("admin".equals(role) || "staff".equals(role)) {
            return true;
        }

        response.sendError(HttpServletResponse.SC_FORBIDDEN);
        return false;
    }
}
