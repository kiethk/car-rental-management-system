package com.crms.mvc.dto;

import com.crms.mvc.enums.EmployeeRole;
import com.crms.mvc.util.EnumTokens;

/** Who is acting on an Issue: the logged-in employee and their role, so the service can enforce admin-only steps. */
public record IssueActor(Long employeeId, String role) {
    public static IssueActor of(SessionAccount account) {
        return new IssueActor(account.getId(), account.getRole());
    }

    public boolean isAdmin() {
        return EnumTokens.is(EmployeeRole.ADMIN, role);
    }
}
