package com.crms.mvc.support;

import com.crms.mvc.dto.SessionAccount;
import com.crms.mvc.util.CurrentAccount;
import org.springframework.mock.web.MockHttpSession;

public final class TestSessions {
    public static final String STAMP = "test-stamp";

    private TestSessions() {
    }

    public static MockHttpSession customer(Long id) {
        return customer(id, "Test Customer");
    }

    public static MockHttpSession customer(Long id, String name) {
        return of(new SessionAccount(id, SessionAccount.CUSTOMER, name, "customer@example.com", null, STAMP));
    }

    public static MockHttpSession staff(Long id) {
        return employee(id, "staff", "Test Staff");
    }

    public static MockHttpSession admin(Long id) {
        return employee(id, "admin", "Test Admin");
    }

    public static MockHttpSession employee(Long id, String role, String name) {
        return of(new SessionAccount(id, SessionAccount.EMPLOYEE, name, "employee@example.com", role, STAMP));
    }

    public static MockHttpSession of(SessionAccount account) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(CurrentAccount.SESSION_KEY, account);
        return session;
    }
}
