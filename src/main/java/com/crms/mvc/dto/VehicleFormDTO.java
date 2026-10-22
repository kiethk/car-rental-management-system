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

    @NotNull(message = "Vui lòng chọn dòng xe")
    private Long vehicleTypeId;

    @NotBlank(message = "Biển số xe không được để trống")
    @Size(max = 20, message = "Biển số xe tối đa 20 ký tự")
    private String plate;

    @Size(max = 100, message = "Tỉnh/thành đăng ký tối đa 100 ký tự")
    private String province;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate registrationExpiry;

    @NotBlank(message = "Màu sắc xe không được để trống")
    @Size(max = 50, message = "Màu sắc tối đa 50 ký tự")
    private String color;

    @NotNull(message = "Năm sản xuất không được để trống")
    @Min(value = 1990, message = "Năm sản xuất không hợp lệ")
    @Max(value = 2100, message = "Năm sản xuất không hợp lệ")
    private Integer year = LocalDate.now().getYear();

    @NotNull(message = "Số km đã đi (odometer) không được để trống")
    @Min(value = 0, message = "Số km không được âm")
    private Integer odometer = 0;

    @NotBlank(message = "Trạng thái xe không được để trống")
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
            dto.setRegistrationExpiry(vehicle.getLicensePlate().getRegistrationExpiry());
        }
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
