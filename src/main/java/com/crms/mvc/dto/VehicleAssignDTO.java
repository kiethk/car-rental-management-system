package com.crms.mvc.dto;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

public class VehicleAssignDTO implements Serializable {
    @NotNull(message = "Vehicle ID is required")
    private Long vehicleId;

    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
}

