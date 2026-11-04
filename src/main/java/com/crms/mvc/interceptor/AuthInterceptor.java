package com.crms.mvc.interceptor;

import com.crms.mvc.dto.SessionAccount;
import com.crms.mvc.util.CurrentAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        Optional<SessionAccount> account = CurrentAccount.of(request.getSession(false));
        if (account.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (account.get().isEmployee() && !path.matches("/licenses/\\d+/image")) {
            String target = path.startsWith("/profile") ? "/admin/profile" : "/admin/dashboard";
            response.sendRedirect(request.getContextPath() + target);
            return false;
        }

        return true;
    }
}
