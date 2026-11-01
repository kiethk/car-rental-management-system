package com.crms.mvc.util;

import com.crms.mvc.dto.SessionAccount;
import jakarta.servlet.http.HttpSession;

import java.util.Optional;

public final class CurrentAccount {
    public static final String SESSION_KEY = "account";

    private CurrentAccount() {
    }

    public static Optional<SessionAccount> of(HttpSession session) {
        if (session == null) {
            return Optional.empty();
        }
        return session.getAttribute(SESSION_KEY) instanceof SessionAccount account
                ? Optional.of(account)
                : Optional.empty();
    }

    public static Optional<Long> customerId(HttpSession session) {
        return of(session).filter(SessionAccount::isCustomer).map(SessionAccount::getId);
    }

    public static Optional<Long> employeeId(HttpSession session) {
        return of(session).filter(SessionAccount::isEmployee).map(SessionAccount::getId);
    }

    public static boolean isEmployee(HttpSession session) {
        return of(session).map(SessionAccount::isEmployee).orElse(false);
    }

    public static boolean isAdmin(HttpSession session) {
        return of(session).map(SessionAccount::isAdmin).orElse(false);
    }
}
