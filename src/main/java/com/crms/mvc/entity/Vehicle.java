package com.crms.mvc.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vehicles")
public class Vehicle {
    public static final String STATUS_AVAILABLE = "available";
    public static final String STATUS_RENTED = "rented";
    public static final String STATUS_MAINTENANCE = "maintenance";
    /** Permanently out of service (total loss or never recovered); never rented again. */
    public static final String STATUS_RETIRED = "retired";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vehicle_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_type_id", nullable = false)
    private VehicleType vehicleType;

    /**
     * Read-only view of the plate currently on the car (valid_to IS NULL). The database guarantees
     * at most one such row per vehicle. Full history is loaded through LicensePlateRepository.
     */
    @OneToMany(mappedBy = "vehicle", fetch = FetchType.LAZY)
    @SQLRestriction("valid_to IS NULL")
    @BatchSize(size = 50)
    private List<LicensePlate> currentPlates = new ArrayList<>();

    @Column(name = "color", nullable = false, length = 50)
    private String color;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "odometer", nullable = false)
    private Integer odometer;

    @Column(name = "registration_expiry")
    private LocalDate registrationExpiry;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    /** The plate currently on the car, or null when it has none (for example while being re-registered). */
    public LicensePlate getLicensePlate() {
        return currentPlates.isEmpty() ? null : currentPlates.get(0);
    }

    /** Keeps the in-memory "current plate" view in sync right after a plate is issued or retired. */
    public void setCurrentPlate(LicensePlate plate) {
        currentPlates.clear();
        if (plate != null) {
            currentPlates.add(plate);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public Integer getOdometer() { return odometer; }
    public void setOdometer(Integer odometer) { this.odometer = odometer; }

    public LocalDate getRegistrationExpiry() { return registrationExpiry; }
    public void setRegistrationExpiry(LocalDate registrationExpiry) { this.registrationExpiry = registrationExpiry; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isRetired() { return STATUS_RETIRED.equalsIgnoreCase(status); }
}
