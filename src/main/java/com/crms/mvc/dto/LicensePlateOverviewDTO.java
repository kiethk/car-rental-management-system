package com.crms.mvc.dto;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class LicensePlateOverviewDTO {
    private Long id;
    private String plate;
    private String province;
    private LocalDate registrationExpiry;
    private Long vehicleId;
    private String vehicleName;
    private String vehicleStatus;
    private String expiryStatus; // "EXPIRED", "EXPIRING_SOON", "VALID", "UNKNOWN"
    private Long daysRemaining;

    public LicensePlateOverviewDTO() {}

    public LicensePlateOverviewDTO(Long id, String plate, String province, LocalDate registrationExpiry,
                                   Long vehicleId, String vehicleName, String vehicleStatus) {
        this.id = id;
        this.plate = plate;
        this.province = province;
        this.registrationExpiry = registrationExpiry;
        this.vehicleId = vehicleId;
        this.vehicleName = vehicleName;
        this.vehicleStatus = vehicleStatus;

        if (registrationExpiry != null) {
            this.daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), registrationExpiry);
            if (this.daysRemaining < 0) {
                this.expiryStatus = "EXPIRED";
            } else if (this.daysRemaining <= 30) {
                this.expiryStatus = "EXPIRING_SOON";
            } else {
                this.expiryStatus = "VALID";
            }
        } else {
            this.daysRemaining = null;
            this.expiryStatus = "UNKNOWN";
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public LocalDate getRegistrationExpiry() { return registrationExpiry; }
    public void setRegistrationExpiry(LocalDate registrationExpiry) { this.registrationExpiry = registrationExpiry; }

    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }

    public String getVehicleName() { return vehicleName; }
    public void setVehicleName(String vehicleName) { this.vehicleName = vehicleName; }

    public String getVehicleStatus() { return vehicleStatus; }
    public void setVehicleStatus(String vehicleStatus) { this.vehicleStatus = vehicleStatus; }

    public String getExpiryStatus() { return expiryStatus; }
    public void setExpiryStatus(String expiryStatus) { this.expiryStatus = expiryStatus; }

    public Long getDaysRemaining() { return daysRemaining; }
    public void setDaysRemaining(Long daysRemaining) { this.daysRemaining = daysRemaining; }
}
