package com.crms.mvc.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

public class CheckOutDTO {
    @NotNull(message = "Odometer is required")
    @Min(value = 0, message = "Odometer cannot be negative")
    private Integer odometer;

    @NotNull(message = "Fuel level is required")
    @Min(value = 0, message = "Fuel level must be between 0 and 100")
    @Max(value = 100, message = "Fuel level must be between 0 and 100")
    private Integer fuelPercent;

    @Size(max = 150, message = "Condition notes must be at most 150 characters")
    private String notes;

    private boolean moveToMaintenance;

    private MultipartFile damageImage;

    @Valid
    private List<SurchargeDTO> surcharges = new ArrayList<>();

    public Integer getOdometer() { return odometer; }
    public void setOdometer(Integer odometer) { this.odometer = odometer; }

    public Integer getFuelPercent() { return fuelPercent; }
    public void setFuelPercent(Integer fuelPercent) { this.fuelPercent = fuelPercent; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isMoveToMaintenance() { return moveToMaintenance; }
    public void setMoveToMaintenance(boolean moveToMaintenance) { this.moveToMaintenance = moveToMaintenance; }

    public MultipartFile getDamageImage() { return damageImage; }
    public void setDamageImage(MultipartFile damageImage) { this.damageImage = damageImage; }

    public List<SurchargeDTO> getSurcharges() { return surcharges; }
    public void setSurcharges(List<SurchargeDTO> surcharges) { this.surcharges = surcharges; }
}
