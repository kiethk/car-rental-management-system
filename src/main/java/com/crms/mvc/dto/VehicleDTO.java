package com.crms.mvc.dto;

import com.crms.mvc.entity.Vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;

public class VehicleDTO {
    private Long id;
    private Long vehicleTypeId;
    private String licensePlate;
    private String province;
    private LocalDate registrationExpiry;
    private String brand;
    private String model;
    private String category;
    private Integer seats;
    private String transmission;
    private String fuelType;
    private String color;
    private Integer year;
    private Integer odometer;
    private BigDecimal pricePerHour;
    private BigDecimal estimatedDeposit;
    private String status;
    private String description;
    private String imageUrl;

    public VehicleDTO() {}

    public VehicleDTO(Long id, String licensePlate, String brand, String model,
                      Integer year, BigDecimal pricePerHour, String status) {
        this.id = id;
        this.licensePlate = licensePlate;
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.pricePerHour = pricePerHour;
        this.status = status;
        if (pricePerHour != null) {
            // Estimate 1-day rental deposit (24h * price * 50%)
            this.estimatedDeposit = pricePerHour.multiply(BigDecimal.valueOf(12));
        }
    }

        public static VehicleDTO fromVehicleType(com.crms.mvc.entity.VehicleType vt) {
        VehicleDTO dto = new VehicleDTO();
        dto.setId(vt.getId());
        dto.setVehicleTypeId(vt.getId());
        dto.setBrand(vt.getBrand());
        dto.setModel(vt.getName());
        dto.setCategory(vt.getCategory());
        dto.setSeats(vt.getSeats());
        dto.setTransmission(vt.getTransmission());
        dto.setFuelType(vt.getFuelType());
        dto.setPricePerHour(vt.getPricePerHour());
        dto.setDescription(vt.getDescription());
        dto.setImageUrl(vt.getImageUrl());
        dto.setStatus("available");
        if (vt.getPricePerHour() != null) {
            dto.setEstimatedDeposit(vt.getPricePerHour().multiply(BigDecimal.valueOf(12)));
        }
        return dto;
    }

    public static VehicleDTO fromEntity(Vehicle vehicle) {
        VehicleDTO dto = new VehicleDTO();
        dto.setId(vehicle.getId());
        dto.setColor(vehicle.getColor());
        dto.setYear(vehicle.getYear());
        dto.setOdometer(vehicle.getOdometer());
        dto.setStatus(vehicle.getStatus());

        if (vehicle.getLicensePlate() != null) {
            dto.setLicensePlate(vehicle.getLicensePlate().getPlate());
            dto.setProvince(vehicle.getLicensePlate().getProvince());
            dto.setRegistrationExpiry(vehicle.getLicensePlate().getRegistrationExpiry());
        } else {
            dto.setLicensePlate("Unassigned");
        }

        if (vehicle.getVehicleType() != null) {
            dto.setVehicleTypeId(vehicle.getVehicleType().getId());
            dto.setBrand(vehicle.getVehicleType().getBrand());
            dto.setModel(vehicle.getVehicleType().getName());
            dto.setCategory(vehicle.getVehicleType().getCategory());
            dto.setSeats(vehicle.getVehicleType().getSeats());
            dto.setTransmission(vehicle.getVehicleType().getTransmission());
            dto.setFuelType(vehicle.getVehicleType().getFuelType());
            dto.setPricePerHour(vehicle.getVehicleType().getPricePerHour());
            dto.setDescription(vehicle.getVehicleType().getDescription());
            dto.setImageUrl(vehicle.getVehicleType().getImageUrl());

            if (vehicle.getVehicleType().getPricePerHour() != null) {
                // 50% of estimated day rental
                dto.setEstimatedDeposit(vehicle.getVehicleType().getPricePerHour().multiply(BigDecimal.valueOf(12)));
            }
        }

        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getVehicleTypeId() { return vehicleTypeId; }
    public void setVehicleTypeId(Long vehicleTypeId) { this.vehicleTypeId = vehicleTypeId; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public LocalDate getRegistrationExpiry() { return registrationExpiry; }
    public void setRegistrationExpiry(LocalDate registrationExpiry) { this.registrationExpiry = registrationExpiry; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getSeats() { return seats; }
    public void setSeats(Integer seats) { this.seats = seats; }

    public String getTransmission() { return transmission; }
    public void setTransmission(String transmission) { this.transmission = transmission; }

    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public Integer getOdometer() { return odometer; }
    public void setOdometer(Integer odometer) { this.odometer = odometer; }

    public BigDecimal getPricePerHour() { return pricePerHour; }
    public void setPricePerHour(BigDecimal pricePerHour) { this.pricePerHour = pricePerHour; }

    public BigDecimal getEstimatedDeposit() { return estimatedDeposit; }
    public void setEstimatedDeposit(BigDecimal estimatedDeposit) { this.estimatedDeposit = estimatedDeposit; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getDisplayName() {
        if (model == null || model.isBlank()) return brand != null ? brand : "";
        if (brand != null && !brand.isBlank()) {
            String trimmedBrand = brand.trim().toLowerCase();
            String trimmedModel = model.trim().toLowerCase();
            if (trimmedModel.startsWith(trimmedBrand)) {
                return model;
            }
            return brand + " " + model;
        }
        return model;
    }
}
