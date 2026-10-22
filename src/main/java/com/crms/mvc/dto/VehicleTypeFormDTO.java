package com.crms.mvc.dto;

import com.crms.mvc.entity.VehicleType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

public class VehicleTypeFormDTO {
    private Long id;

    @NotBlank(message = "Model name cannot be blank")
    @Size(max = 100, message = "Model name cannot exceed 100 characters")
    private String name;

    @NotBlank(message = "Brand cannot be blank")
    @Size(max = 100, message = "Brand cannot exceed 100 characters")
    private String brand;

    @NotBlank(message = "Category cannot be blank")
    @Size(max = 30, message = "Category cannot exceed 30 characters")
    private String category;

    @NotNull(message = "Seat capacity cannot be empty")
    @Min(value = 1, message = "Seat capacity must be greater than 0")
    private Integer seats;

    @NotBlank(message = "Transmission cannot be blank")
    @Size(max = 20, message = "Transmission cannot exceed 20 characters")
    private String transmission;

    @NotBlank(message = "Fuel type cannot be blank")
    @Size(max = 20, message = "Fuel type cannot exceed 20 characters")
    private String fuelType;

    @NotNull(message = "Hourly price cannot be empty")
    @DecimalMin(value = "0.00", message = "Hourly price cannot be negative")
    private BigDecimal pricePerHour;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    @Size(max = 500, message = "Image URL cannot exceed 500 characters")
    private String imageUrl;

    private MultipartFile imageFile;

    public VehicleTypeFormDTO() {}

    public static VehicleTypeFormDTO fromEntity(VehicleType vt) {
        VehicleTypeFormDTO dto = new VehicleTypeFormDTO();
        dto.setId(vt.getId());
        dto.setName(vt.getName());
        dto.setBrand(vt.getBrand());
        dto.setCategory(vt.getCategory());
        dto.setSeats(vt.getSeats());
        dto.setTransmission(vt.getTransmission());
        dto.setFuelType(vt.getFuelType());
        dto.setPricePerHour(vt.getPricePerHour());
        dto.setDescription(vt.getDescription());
        dto.setImageUrl(vt.getImageUrl());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getSeats() { return seats; }
    public void setSeats(Integer seats) { this.seats = seats; }

    public String getTransmission() { return transmission; }
    public void setTransmission(String transmission) { this.transmission = transmission; }

    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }

    public BigDecimal getPricePerHour() { return pricePerHour; }
    public void setPricePerHour(BigDecimal pricePerHour) { this.pricePerHour = pricePerHour; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public MultipartFile getImageFile() { return imageFile; }
    public void setImageFile(MultipartFile imageFile) { this.imageFile = imageFile; }
}
