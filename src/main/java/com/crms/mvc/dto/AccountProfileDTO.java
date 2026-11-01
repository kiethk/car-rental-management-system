package com.crms.mvc.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AccountProfileDTO {
    private String name;
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^(0\\d{9}|\\+84\\d{9})$", message = "Phone number is invalid")
    private String phone;

    @Size(max = 255, message = "Password is too long")
    private String currentPassword;

    @Size(max = 255, message = "Password is too long")
    private String newPassword;

    @Size(max = 255, message = "Password is too long")
    private String confirmPassword;

    public boolean isPasswordChangeRequested() {
        return newPassword != null && !newPassword.isBlank();
    }

    @AssertTrue(message = "Current password is required")
    public boolean isCurrentPasswordProvided() {
        return !isPasswordChangeRequested() || (currentPassword != null && !currentPassword.isBlank());
    }

    @AssertTrue(message = "New password must be at least 8 characters")
    public boolean isNewPasswordLongEnough() {
        return !isPasswordChangeRequested() || newPassword.length() >= 8;
    }

    @AssertTrue(message = "Passwords do not match")
    public boolean isPasswordConfirmed() {
        return !isPasswordChangeRequested() || newPassword.equals(confirmPassword);
    }

    @AssertTrue(message = "New password must be different from the current password")
    public boolean isNewPasswordDifferent() {
        return !isPasswordChangeRequested() || !newPassword.equals(currentPassword);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

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

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
