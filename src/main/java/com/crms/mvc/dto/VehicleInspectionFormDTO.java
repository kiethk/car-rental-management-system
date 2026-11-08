package com.crms.mvc.dto;

import com.crms.mvc.entity.VehicleInspection;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class VehicleInspectionFormDTO {
    private Long id;

    @NotNull(message = "Vui lòng chọn xe cần đăng kiểm")
    private Long vehicleId;

    private String vehiclePlate;
    private String vehicleName;

    /** The day the car goes in for inspection; the whole day is blocked for rentals, so no time is asked. */
    @NotNull(message = "Vui lòng chọn ngày đăng kiểm")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate scheduledDate = LocalDate.now();

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime completedAt;

    private String status = "scheduled"; // scheduled, passed, failed

    @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
    private String notes;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate newRegistrationExpiry;

    public VehicleInspectionFormDTO() {}

    public static VehicleInspectionFormDTO fromEntity(VehicleInspection inspection) {
        VehicleInspectionFormDTO dto = new VehicleInspectionFormDTO();
        dto.setId(inspection.getId());
        if (inspection.getVehicle() != null) {
            dto.setVehicleId(inspection.getVehicle().getId());
            if (inspection.getVehicle().getLicensePlate() != null) {
                dto.setVehiclePlate(inspection.getVehicle().getLicensePlate().getPlate());
            }
            dto.setNewRegistrationExpiry(inspection.getVehicle().getRegistrationExpiry());
            if (inspection.getVehicle().getVehicleType() != null) {
                dto.setVehicleName(inspection.getVehicle().getVehicleType().getBrand() + " " + inspection.getVehicle().getVehicleType().getName());
            }
        }
        if (inspection.getScheduledDate() != null) {
            dto.setScheduledDate(inspection.getScheduledDate());
        }
        if (inspection.getCompletedAt() != null) {
            dto.setCompletedAt(inspection.getCompletedAt().toLocalDateTime());
        }
        dto.setStatus(inspection.getStatus());
        dto.setNotes(inspection.getNotes());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }

    public String getVehiclePlate() { return vehiclePlate; }
    public void setVehiclePlate(String vehiclePlate) { this.vehiclePlate = vehiclePlate; }

    public String getVehicleName() { return vehicleName; }
    public void setVehicleName(String vehicleName) { this.vehicleName = vehicleName; }

    public LocalDate getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDate scheduledDate) { this.scheduledDate = scheduledDate; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDate getNewRegistrationExpiry() { return newRegistrationExpiry; }
    public void setNewRegistrationExpiry(LocalDate newRegistrationExpiry) { this.newRegistrationExpiry = newRegistrationExpiry; }
}
