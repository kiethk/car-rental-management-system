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

    @NotBlank(message = "Tên dòng xe không được để trống")
    @Size(max = 100, message = "Tên dòng xe tối đa 100 ký tự")
    private String name;

    @NotBlank(message = "Hãng xe không được để trống")
    @Size(max = 100, message = "Hãng xe tối đa 100 ký tự")
    private String brand;

    @NotBlank(message = "Phân khúc xe không được để trống")
    @Size(max = 30, message = "Phân khúc xe tối đa 30 ký tự")
    private String category;

    @NotNull(message = "Số chỗ ngồi không được để trống")
    @Min(value = 1, message = "Số chỗ ngồi phải lớn hơn 0")
    private Integer seats;

    @NotBlank(message = "Hộp số không được để trống")
    @Size(max = 20, message = "Hộp số tối đa 20 ký tự")
    private String transmission;

    @NotBlank(message = "Nhiên liệu không được để trống")
    @Size(max = 20, message = "Nhiên liệu tối đa 20 ký tự")
    private String fuelType;

    @NotNull(message = "Giá thuê mỗi giờ không được để trống")
    @DecimalMin(value = "0.00", message = "Giá thuê mỗi giờ không được âm")
    private BigDecimal pricePerHour;

    @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
    private String description;

    @Size(max = 500, message = "Đường dẫn ảnh tối đa 500 ký tự")
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
