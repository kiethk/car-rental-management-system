package com.crms.mvc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Admin form: put a new plate on a vehicle. */
public class LicensePlateChangeDTO {
    @NotBlank(message = "New license plate cannot be blank")
    @Size(max = 20, message = "License plate cannot exceed 20 characters")
    @Pattern(regexp = "^[A-Za-z0-9 .\\-]*$", message = "License plate may only contain letters, digits, spaces, dots and dashes")
    private String plate;

    @Size(max = 100, message = "Province cannot exceed 100 characters")
    private String province;

    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
}
