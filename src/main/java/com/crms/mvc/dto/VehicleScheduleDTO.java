package com.crms.mvc.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * When one physical car is taken over the coming days, hour by hour, so a customer can pick a free window.
 * Shows no customer data: only whether each hour is booked, held as the 1-hour buffer, free or already past.
 */
public class VehicleScheduleDTO implements Serializable {

    public enum HourState { FREE, BOOKED, BUFFER, PAST }

    public record Day(LocalDate date, List<HourState> hours) implements Serializable {
    }

    private Long vehicleId;
    private String licensePlate;
    private Long vehicleTypeId;
    private String vehicleTypeName;
    private String vehicleStatus;
    private List<Day> days;

    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public Long getVehicleTypeId() { return vehicleTypeId; }
    public void setVehicleTypeId(Long vehicleTypeId) { this.vehicleTypeId = vehicleTypeId; }

    public String getVehicleTypeName() { return vehicleTypeName; }
    public void setVehicleTypeName(String vehicleTypeName) { this.vehicleTypeName = vehicleTypeName; }

    public String getVehicleStatus() { return vehicleStatus; }
    public void setVehicleStatus(String vehicleStatus) { this.vehicleStatus = vehicleStatus; }

    public List<Day> getDays() { return days; }
    public void setDays(List<Day> days) { this.days = days; }
}
