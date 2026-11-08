package com.crms.mvc.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public class LicenseSubmitDTO {
    @NotBlank(message = "Số bằng lái xe không được để trống (License number is required)")
    @Size(max = 50, message = "Số bằng lái xe tối đa 50 ký tự")
    @Pattern(
            regexp = "^(?:[0-9]{12}|[A-Za-z0-9]{2}-?[0-9]{6,10})$",
            message = "Số GPLX phải gồm 12 chữ số hoặc đúng định dạng (VD: B2-12345678 hoặc 079123456789)"
    )
    private String licenseNumber;

    @NotBlank(message = "Hạng bằng lái không được để trống (License class is required)")
    @Size(max = 20, message = "Hạng bằng lái tối đa 20 ký tự")
    @Pattern(
            regexp = "^(?i)(A1|A2|A3|A4|B|B1|B2|C|C1|D|D1|D2|E|BE|CE|DE|FB2|FC|FD|FE)$",
            message = "Hạng GPLX không hợp lệ (VD: B1, B2, B, C, D, E...)"
    )
    private String licenseClass;

    @NotNull(message = "Ngày hết hạn không được để trống (License expiry date is required)")
    @Future(message = "Thời hạn bằng lái xe phải còn hiệu lực ở tương lai (Expiry date must be in the future)")
    private LocalDate licenseExpiry;

    private MultipartFile licenseImage;

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getLicenseClass() {
        return licenseClass;
    }

    public void setLicenseClass(String licenseClass) {
        this.licenseClass = licenseClass;
    }

    public LocalDate getLicenseExpiry() {
        return licenseExpiry;
    }

    public void setLicenseExpiry(LocalDate licenseExpiry) {
        this.licenseExpiry = licenseExpiry;
    }

    public MultipartFile getLicenseImage() {
        return licenseImage;
    }

    public void setLicenseImage(MultipartFile licenseImage) {
        this.licenseImage = licenseImage;
    }
}
