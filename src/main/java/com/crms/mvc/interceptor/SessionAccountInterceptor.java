package com.crms.mvc.interceptor;

import com.crms.mvc.dto.SessionAccount;
import com.crms.mvc.service.SessionAccountService;
import com.crms.mvc.util.CurrentAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Component
public class SessionAccountInterceptor implements HandlerInterceptor {
    private final SessionAccountService sessionAccountService;

    public SessionAccountInterceptor(SessionAccountService sessionAccountService) {
        this.sessionAccountService = sessionAccountService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return true;
        }

        Object stored = session.getAttribute(CurrentAccount.SESSION_KEY);
        if (stored == null) {
            return true;
        }
        if (!(stored instanceof SessionAccount account)) {
            session.removeAttribute(CurrentAccount.SESSION_KEY);
            return true;
        }

        Optional<SessionAccount> refreshed = sessionAccountService.refresh(account);
        if (refreshed.isEmpty()) {
            session.invalidate();
            return true;
        }

        session.setAttribute(CurrentAccount.SESSION_KEY, refreshed.get());
        return true;
    }
}
