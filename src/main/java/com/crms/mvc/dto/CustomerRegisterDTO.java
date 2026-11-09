package com.crms.mvc.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CustomerRegisterDTO {
    @NotBlank(message = "Vui lòng nhập họ tên")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String name;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = "^(0\\d{9}|\\+84\\d{9})$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không hợp lệ")
    @Size(max = 150, message = "Email tối đa 150 ký tự")
    private String email;

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    @Size(min = 8, max = 255, message = "Mật khẩu phải có ít nhất 8 ký tự")
    private String password;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
