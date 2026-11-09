package com.crms.mvc.enums;

/** What an employee may do in the back office. Stored by name. */
public enum EmployeeRole {
    ADMIN("Quản trị viên"),
    STAFF("Nhân viên");

    private final String label;

    EmployeeRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** True for a role text (any case) that names a back-office role. */
    public static boolean isEmployee(String role) {
        return from(role) != null;
    }

    /** Reads a form or query value such as "sedan" or "SEDAN"; null or unknown gives null. */
    public static EmployeeRole from(String value) {
        if (value == null) {
            return null;
        }
        for (EmployeeRole item : values()) {
            if (item.name().equalsIgnoreCase(value.trim())) {
                return item;
            }
        }
        return null;
    }
}
