package com.crms.mvc.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AccountProfileDTO {
    private String name;
    private String email;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = "^(0\\d{9}|\\+84\\d{9})$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @Size(max = 255, message = "Mật khẩu quá dài")
    private String currentPassword;

    @Size(max = 255, message = "Mật khẩu quá dài")
    private String newPassword;

    @Size(max = 255, message = "Mật khẩu quá dài")
    private String confirmPassword;

    public boolean isPasswordChangeRequested() {
        return newPassword != null && !newPassword.isBlank();
    }

    @AssertTrue(message = "Vui lòng nhập mật khẩu hiện tại")
    public boolean isCurrentPasswordProvided() {
        return !isPasswordChangeRequested() || (currentPassword != null && !currentPassword.isBlank());
    }

    @AssertTrue(message = "Mật khẩu mới phải có ít nhất 8 ký tự")
    public boolean isNewPasswordLongEnough() {
        return !isPasswordChangeRequested() || newPassword.length() >= 8;
    }

    @AssertTrue(message = "Mật khẩu xác nhận không khớp")
    public boolean isPasswordConfirmed() {
        return !isPasswordChangeRequested() || newPassword.equals(confirmPassword);
    }

    @AssertTrue(message = "Mật khẩu mới phải khác mật khẩu hiện tại")
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
