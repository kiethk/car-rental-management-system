package com.crms.mvc.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CheckOutDTO implements Serializable {
    @NotNull(message = "Vui lòng nhập số km")
    @Min(value = 0, message = "Số km không được âm")
    private Integer odometer;

    @NotNull(message = "Vui lòng nhập mức nhiên liệu")
    @Min(value = 0, message = "Mức nhiên liệu phải từ 0 đến 100")
    @Max(value = 100, message = "Mức nhiên liệu phải từ 0 đến 100")
    private Integer fuelPercent;

    private boolean moveToMaintenance;

    @Valid
    private List<ReturnChargeDTO> returnCharges = new ArrayList<>();

    public Integer getOdometer() { return odometer; }
    public void setOdometer(Integer odometer) { this.odometer = odometer; }

    public Integer getFuelPercent() { return fuelPercent; }
    public void setFuelPercent(Integer fuelPercent) { this.fuelPercent = fuelPercent; }

    public boolean isMoveToMaintenance() { return moveToMaintenance; }
    public void setMoveToMaintenance(boolean moveToMaintenance) { this.moveToMaintenance = moveToMaintenance; }

    public List<ReturnChargeDTO> getReturnCharges() { return returnCharges; }
    public void setReturnCharges(List<ReturnChargeDTO> returnCharges) { this.returnCharges = returnCharges; }
}
