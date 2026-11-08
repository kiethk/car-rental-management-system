package com.crms.mvc.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

public class CheckOutDTO implements Serializable {
    @NotNull(message = "Vui lòng nhập số km")
    @Min(value = 0, message = "Số km không được âm")
    private Integer odometer;

    private boolean moveToMaintenance;

    public Integer getOdometer() { return odometer; }
    public void setOdometer(Integer odometer) { this.odometer = odometer; }

    public boolean isMoveToMaintenance() { return moveToMaintenance; }
    public void setMoveToMaintenance(boolean moveToMaintenance) { this.moveToMaintenance = moveToMaintenance; }
}
