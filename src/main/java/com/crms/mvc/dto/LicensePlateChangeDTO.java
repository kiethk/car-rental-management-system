package com.crms.mvc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Admin form: put a new plate on a vehicle. */
public class LicensePlateChangeDTO {
    @NotBlank(message = "Vui lòng nhập biển số mới")
    @Size(max = 20, message = "Biển số tối đa 20 ký tự")
    @Pattern(regexp = "^[A-Za-z0-9 .\\-]*$", message = "Biển số chỉ được chứa chữ cái, chữ số, khoảng trắng, dấu chấm và dấu gạch ngang")
    private String plate;

    @Size(max = 100, message = "Tỉnh / thành phố tối đa 100 ký tự")
    private String province;

    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
}
