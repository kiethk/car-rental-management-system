package com.crms.mvc.dto;

import java.time.OffsetDateTime;

public class HandoverRecordDTO {
    private Long id;
    private String type;
    private String typeLabel;
    private Integer odometer;
    private Integer fuelPercent;
    private String notes;
    private String employeeName;
    /** Already shifted to Vietnam time for display. */
    private OffsetDateTime recordedAt;
    private boolean licenseImage;
    private boolean idCardImage;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTypeLabel() { return typeLabel; }
    public void setTypeLabel(String typeLabel) { this.typeLabel = typeLabel; }

    public Integer getOdometer() { return odometer; }
    public void setOdometer(Integer odometer) { this.odometer = odometer; }

    public Integer getFuelPercent() { return fuelPercent; }
    public void setFuelPercent(Integer fuelPercent) { this.fuelPercent = fuelPercent; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public OffsetDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(OffsetDateTime recordedAt) { this.recordedAt = recordedAt; }

    public boolean isLicenseImage() { return licenseImage; }
    public void setLicenseImage(boolean licenseImage) { this.licenseImage = licenseImage; }

    public boolean isIdCardImage() { return idCardImage; }
    public void setIdCardImage(boolean idCardImage) { this.idCardImage = idCardImage; }
}
