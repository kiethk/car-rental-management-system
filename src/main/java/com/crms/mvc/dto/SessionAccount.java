package com.crms.mvc.dto;

import com.crms.mvc.util.EmployeeRole;

import java.io.Serializable;

public final class SessionAccount implements Serializable {
    public static final String CUSTOMER = "customer";
    public static final String EMPLOYEE = "employee";

    private final Long id;
    private final String accountType;
    private final String name;
    private final String email;
    private final String role;
    private final String credentialStamp;

    public SessionAccount(Long id, String accountType, String name, String email,
                          String role, String credentialStamp) {
        this.id = id;
        this.accountType = accountType;
        this.name = name;
        this.email = email;
        this.role = role;
        this.credentialStamp = credentialStamp;
    }

    public Long getId() { return id; }
    public String getAccountType() { return accountType; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getCredentialStamp() { return credentialStamp; }

    public boolean isCustomer() {
        return CUSTOMER.equals(accountType);
    }

    public boolean isEmployee() {
        return EMPLOYEE.equals(accountType);
    }

    public boolean isAdmin() {
        return isEmployee() && EmployeeRole.ADMIN.equals(role);
    }

    public boolean isStaffOrAdmin() {
        return isEmployee() && EmployeeRole.isEmployee(role);
    }
}
