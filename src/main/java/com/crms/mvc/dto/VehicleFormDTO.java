package com.crms.mvc.dto;

import com.crms.mvc.entity.Vehicle;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class VehicleFormDTO {
    private Long id;

    @NotNull(message = "Please select a vehicle model")
    private Long vehicleTypeId;

    @NotBlank(message = "License plate cannot be blank")
    @Size(max = 20, message = "License plate cannot exceed 20 characters")
    private String plate;

    @Size(max = 100, message = "Province cannot exceed 100 characters")
    private String province;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate registrationExpiry;

    @NotBlank(message = "Color cannot be blank")
    @Size(max = 50, message = "Color cannot exceed 50 characters")
    private String color;

    @NotNull(message = "Manufacturing year cannot be empty")
    @Min(value = 1990, message = "Manufacturing year is invalid")
    @Max(value = 2100, message = "Manufacturing year is invalid")
    private Integer year = LocalDate.now().getYear();

    @NotNull(message = "Odometer cannot be empty")
    @Min(value = 0, message = "Odometer cannot be negative")
    private Integer odometer = 0;

    @NotBlank(message = "Vehicle status cannot be blank")
    private String status = "available";

    public VehicleFormDTO() {}

    public static VehicleFormDTO fromEntity(Vehicle vehicle) {
        VehicleFormDTO dto = new VehicleFormDTO();
        dto.setId(vehicle.getId());
        if (vehicle.getVehicleType() != null) {
            dto.setVehicleTypeId(vehicle.getVehicleType().getId());
        }
        if (vehicle.getLicensePlate() != null) {
            dto.setPlate(vehicle.getLicensePlate().getPlate());
            dto.setProvince(vehicle.getLicensePlate().getProvince());
        }
        dto.setRegistrationExpiry(vehicle.getRegistrationExpiry());
        dto.setColor(vehicle.getColor());
        dto.setYear(vehicle.getYear());
        dto.setOdometer(vehicle.getOdometer());
        dto.setStatus(vehicle.getStatus());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getVehicleTypeId() { return vehicleTypeId; }
    public void setVehicleTypeId(Long vehicleTypeId) { this.vehicleTypeId = vehicleTypeId; }

    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public LocalDate getRegistrationExpiry() { return registrationExpiry; }
    public void setRegistrationExpiry(LocalDate registrationExpiry) { this.registrationExpiry = registrationExpiry; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public Integer getOdometer() { return odometer; }
    public void setOdometer(Integer odometer) { this.odometer = odometer; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
