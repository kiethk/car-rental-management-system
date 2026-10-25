package com.crms.mvc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PasswordChangeDTO {
    @NotBlank(message = "Current password is required")
    @Size(max = 255, message = "Password is too long")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 255, message = "New password must be at least 8 characters")
    private String newPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
