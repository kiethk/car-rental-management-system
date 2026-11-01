package com.crms.mvc.util;

import jakarta.servlet.http.HttpSession;

/** Roles an employee can have, as stored in {@code employees.role} and in the session. */
public final class EmployeeRole {
    public static final String ADMIN = "admin";
    public static final String STAFF = "staff";

    public static final String SESSION_KEY_ROLE = "employeeRole";
    public static final String SESSION_KEY_ID = "employeeId";

    private EmployeeRole() {}

    public static boolean isAdmin(HttpSession session) {
        return session != null && ADMIN.equals(session.getAttribute(SESSION_KEY_ROLE));
    }

    /** True for any logged-in back-office role (admin or staff). */
    public static boolean isEmployee(String role) {
        return ADMIN.equals(role) || STAFF.equals(role);
    }
}
