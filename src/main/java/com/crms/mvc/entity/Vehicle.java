package com.crms.mvc.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "vehicles")
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vehicle_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_type_id", nullable = false)
    private VehicleType vehicleType;

    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "license_plate_id", nullable = true, unique = true)
    private LicensePlate licensePlate;

    @Column(name = "color", nullable = false, length = 50)
    private String color;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "odometer", nullable = false)
    private Integer odometer;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }

    public LicensePlate getLicensePlate() { return licensePlate; }
    public void setLicensePlate(LicensePlate licensePlate) { this.licensePlate = licensePlate; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public Integer getOdometer() { return odometer; }
    public void setOdometer(Integer odometer) { this.odometer = odometer; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
