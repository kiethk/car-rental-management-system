package com.crms.mvc.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "vehicle_types")
public class VehicleType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vehicle_type_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "brand", nullable = false, length = 100)
    private String brand;

    @Column(name = "category", nullable = false, length = 30)
    private String category;

    @Column(name = "seats", nullable = false)
    private Integer seats;

    @Column(name = "transmission", nullable = false, length = 20)
    private String transmission;

    @Column(name = "fuel_type", nullable = false, length = 20)
    private String fuelType;

    @Column(name = "price_per_hour", nullable = false, precision = 14, scale = 2)
    private BigDecimal pricePerHour;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getSeats() { return seats; }
    public void setSeats(Integer seats) { this.seats = seats; }

    public String getTransmission() { return transmission; }
    public void setTransmission(String transmission) { this.transmission = transmission; }

    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }

    public BigDecimal getPricePerHour() { return pricePerHour; }
    public void setPricePerHour(BigDecimal pricePerHour) { this.pricePerHour = pricePerHour; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getDisplayName() {
        if (name == null || name.isBlank()) return brand != null ? brand : "";
        if (brand != null && !brand.isBlank()) {
            String trimmedBrand = brand.trim().toLowerCase();
            String trimmedName = name.trim().toLowerCase();
            if (trimmedName.startsWith(trimmedBrand)) {
                return name;
            }
            return brand + " " + name;
        }
        return name;
    }
}
