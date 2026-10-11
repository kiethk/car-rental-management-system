package com.crms.mvc.dto;
import com.crms.mvc.entity.Vehicle;
import java.math.BigDecimal;

public record VehicleDTO(Long id, String licensePlate, String brand, String model,
                         Integer year, BigDecimal pricePerHour, String status) {
    public static VehicleDTO fromEntity(Vehicle vehicle) {
        return new VehicleDTO(vehicle.getId(), vehicle.getLicensePlate() == null ? "Chưa gán" : vehicle.getLicensePlate().getPlate(),
                vehicle.getVehicleType().getBrand(), vehicle.getVehicleType().getName(),
                vehicle.getYear(), vehicle.getVehicleType().getPricePerHour(), vehicle.getStatus());
    }
}
