package com.crms.mvc.interceptor;

import com.crms.mvc.dto.SessionAccount;
import com.crms.mvc.util.CurrentAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RoleInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        boolean allowed = CurrentAccount.of(request.getSession(false))
                .map(SessionAccount::isStaffOrAdmin)
                .orElse(false);
        if (allowed) {
            return true;
        }

        response.sendError(HttpServletResponse.SC_FORBIDDEN);
        return false;
    }
}
