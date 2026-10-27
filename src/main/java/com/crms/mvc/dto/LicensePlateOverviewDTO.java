package com.crms.mvc.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

/**
 * One row of the "plates and registration" monitor: a vehicle, the plate currently on it
 * and when the vehicle's registration (inspection sticker) runs out.
 */
public class LicensePlateOverviewDTO {
    private Long vehicleId;
    private String vehicleName;
    private String vehicleStatus;
    private String plate;                 // null when the vehicle currently has no plate
    private String province;
    private OffsetDateTime plateSince;
    private LocalDate registrationExpiry;
    private String expiryStatus;          // "EXPIRED", "EXPIRING_SOON", "VALID", "UNKNOWN"
    private Long daysRemaining;

    public LicensePlateOverviewDTO() {}

    public LicensePlateOverviewDTO(Long vehicleId, String vehicleName, String vehicleStatus,
                                   String plate, String province, OffsetDateTime plateSince,
                                   LocalDate registrationExpiry) {
        this.vehicleId = vehicleId;
        this.vehicleName = vehicleName;
        this.vehicleStatus = vehicleStatus;
        this.plate = plate;
        this.province = province;
        this.plateSince = plateSince;
        this.registrationExpiry = registrationExpiry;
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

    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }

    public String getVehicleName() { return vehicleName; }
    public void setVehicleName(String vehicleName) { this.vehicleName = vehicleName; }

    public String getVehicleStatus() { return vehicleStatus; }
    public void setVehicleStatus(String vehicleStatus) { this.vehicleStatus = vehicleStatus; }

    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public OffsetDateTime getPlateSince() { return plateSince; }
    public void setPlateSince(OffsetDateTime plateSince) { this.plateSince = plateSince; }

    public LocalDate getRegistrationExpiry() { return registrationExpiry; }
    public void setRegistrationExpiry(LocalDate registrationExpiry) { this.registrationExpiry = registrationExpiry; }

    public String getExpiryStatus() { return expiryStatus; }
    public void setExpiryStatus(String expiryStatus) { this.expiryStatus = expiryStatus; }

    public Long getDaysRemaining() { return daysRemaining; }
    public void setDaysRemaining(Long daysRemaining) { this.daysRemaining = daysRemaining; }
}
