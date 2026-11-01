package com.crms.mvc.interceptor;

import com.crms.mvc.dto.SessionAccount;
import com.crms.mvc.util.CurrentAccount;
import com.crms.mvc.util.EmployeeRole;
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
        boolean allowed = CurrentAccount.of(session)
                .map(SessionAccount::isStaffOrAdmin)
                .orElse(false);
        if (!allowed && session != null) {
            String role = (String) session.getAttribute("employeeRole");
            allowed = EmployeeRole.isEmployee(role);
        }
        if (allowed) {
            return true;
        }

        response.sendError(HttpServletResponse.SC_FORBIDDEN);
        return false;
    }
}
