package com.crms.mvc.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "license_plates")
public class LicensePlate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "license_plate_id")
    private Long id;

    @Column(name = "plate", nullable = false, unique = true, length = 20)
    private String plate;

    @Column(name = "province", length = 100)
    private String province;

    @Column(name = "registration_expiry")
    private LocalDate registrationExpiry;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public LocalDate getRegistrationExpiry() { return registrationExpiry; }
    public void setRegistrationExpiry(LocalDate registrationExpiry) { this.registrationExpiry = registrationExpiry; }
}
