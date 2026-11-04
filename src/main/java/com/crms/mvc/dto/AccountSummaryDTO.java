package com.crms.mvc.dto;

public class AccountSummaryDTO {
    private final Long id;
    private final String type;
    private final String name;
    private final String email;
    private final String phone;
    private final String role;
    private final boolean active;

    public AccountSummaryDTO(Long id, String type, String name, String email, String phone, String role, boolean active) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.active = active;
    }

    public Long getId() { return id; }
    public String getType() { return type; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }
    public boolean isActive() { return active; }
}
