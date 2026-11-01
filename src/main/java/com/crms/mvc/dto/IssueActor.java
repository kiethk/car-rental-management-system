package com.crms.mvc.dto;

import com.crms.mvc.util.EmployeeRole;

/** Who is acting on an Issue: the logged-in employee and their role, so the service can enforce admin-only steps. */
public record IssueActor(Long employeeId, String role) {
    public boolean isAdmin() {
        return EmployeeRole.ADMIN.equals(role);
    }
}
