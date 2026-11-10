package com.crms.mvc.dto;

import com.crms.mvc.util.EnumTokens;
import com.crms.mvc.enums.VehicleStatus;
import com.crms.mvc.entity.Vehicle;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class VehicleFormDTO {
    private Long id;

    @NotNull(message = "Vui lòng chọn dòng xe")
    private Long vehicleTypeId;

    @NotBlank(message = "Vui lòng nhập biển số")
    @Size(max = 20, message = "Biển số tối đa 20 ký tự")
    private String plate;

    @Size(max = 100, message = "Tỉnh / thành phố tối đa 100 ký tự")
    private String province;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate registrationExpiry;

    @NotBlank(message = "Vui lòng nhập màu xe")
    @Size(max = 50, message = "Màu xe tối đa 50 ký tự")
    private String color;

    @NotNull(message = "Vui lòng nhập năm sản xuất")
    @Min(value = 1990, message = "Năm sản xuất không hợp lệ")
    @Max(value = 2100, message = "Năm sản xuất không hợp lệ")
    private Integer year = LocalDate.now().getYear();

    @NotNull(message = "Vui lòng nhập số km")
    @Min(value = 0, message = "Số km không được âm")
    private Integer odometer = 0;

    @NotBlank(message = "Vui lòng chọn trạng thái xe")
    @Pattern(regexp = "^(available|maintenance|retired)$", message = "Trạng thái xe phải là sẵn sàng, bảo dưỡng hoặc ngừng khai thác")
    private String status = EnumTokens.of(VehicleStatus.AVAILABLE);

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
        dto.setStatus(EnumTokens.of(vehicle.getStatus()));
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
