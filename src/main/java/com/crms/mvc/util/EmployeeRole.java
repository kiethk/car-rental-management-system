package com.crms.mvc.util;

/** Roles an employee can have, as stored in {@code employees.role} and in {@link com.crms.mvc.dto.SessionAccount}. */
public final class EmployeeRole {
    public static final String ADMIN = "admin";
    public static final String STAFF = "staff";

    private EmployeeRole() {}

    /** True for any back-office role (admin or staff). */
    public static boolean isEmployee(String role) {
        return ADMIN.equals(role) || STAFF.equals(role);
    }
}
