package com.crms.mvc.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public class LicenseSubmitDTO {
    @NotBlank(message = "License number is required")
    @Size(max = 50, message = "License number must be at most 50 characters")
    private String licenseNumber;

    @NotBlank(message = "License class is required")
    @Size(max = 20, message = "License class must be at most 20 characters")
    private String licenseClass;

    @NotNull(message = "License expiry date is required")
    @Future(message = "License expiry date must be in the future")
    private LocalDate licenseExpiry;

    @NotNull(message = "License image is required")
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
