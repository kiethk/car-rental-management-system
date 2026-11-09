package com.crms.mvc.interceptor;

import com.crms.mvc.dto.SessionAccount;
import com.crms.mvc.util.CurrentAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Component
public class RoleInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        Optional<SessionAccount> account = CurrentAccount.of(request.getSession(false));
        // Not logged in, or the session expired: send them to log in instead of a bare 403.
        if (account.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }
        if (account.get().isStaffOrAdmin()) {
            return true;
        }

        response.sendError(HttpServletResponse.SC_FORBIDDEN);
        return false;
    }
}
