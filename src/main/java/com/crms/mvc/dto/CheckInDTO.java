package com.crms.mvc.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public class CheckInDTO {
    @NotNull(message = "Vui lòng nhập số km")
    @Min(value = 0, message = "Số km không được âm")
    private Integer odometer;

    @NotNull(message = "Vui lòng nhập mức nhiên liệu")
    @Min(value = 0, message = "Mức nhiên liệu phải từ 0 đến 100")
    @Max(value = 100, message = "Mức nhiên liệu phải từ 0 đến 100")
    private Integer fuelPercent;

    @Size(max = 150, message = "Ghi chú tình trạng xe tối đa 150 ký tự")
    private String notes;

    @AssertTrue(message = "Vui lòng xác nhận đã kiểm tra bản gốc GPLX và CCCD")
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
