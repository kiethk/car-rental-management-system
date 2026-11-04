package com.crms.mvc.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public class CheckInDTO {
    @NotNull(message = "Odometer is required")
    @Min(value = 0, message = "Odometer cannot be negative")
    private Integer odometer;

    @NotNull(message = "Fuel level is required")
    @Min(value = 0, message = "Fuel level must be between 0 and 100")
    @Max(value = 100, message = "Fuel level must be between 0 and 100")
    private Integer fuelPercent;

    @Size(max = 150, message = "Condition notes must be at most 150 characters")
    private String notes;

    @AssertTrue(message = "Confirm that the original license and ID card were checked")
    private boolean documentsVerified;

    private MultipartFile licenseImage;
    private MultipartFile idCardImage;

    public Integer getOdometer() { return odometer; }
    public void setOdometer(Integer odometer) { this.odometer = odometer; }

    public Integer getFuelPercent() { return fuelPercent; }
    public void setFuelPercent(Integer fuelPercent) { this.fuelPercent = fuelPercent; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isDocumentsVerified() { return documentsVerified; }
    public void setDocumentsVerified(boolean documentsVerified) { this.documentsVerified = documentsVerified; }

    public MultipartFile getLicenseImage() { return licenseImage; }
    public void setLicenseImage(MultipartFile licenseImage) { this.licenseImage = licenseImage; }

    public MultipartFile getIdCardImage() { return idCardImage; }
    public void setIdCardImage(MultipartFile idCardImage) { this.idCardImage = idCardImage; }
}
