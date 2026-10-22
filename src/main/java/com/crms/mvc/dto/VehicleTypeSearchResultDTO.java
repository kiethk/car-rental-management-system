package com.crms.mvc.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class VehicleTypeSearchResultDTO implements Serializable {
    private Long id;
    private String name;
    private String brand;
    private String category;
    private Integer seats;
    private String transmission;
    private String fuelType;
    private BigDecimal pricePerHour;
    private BigDecimal depositAmount;
    private String imageUrl;
    private int availableCount;
    private BigDecimal estimatedTotal;
    private double rating = 4.7;
    private int reviewCount = 120;

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

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

    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public int getAvailableCount() { return availableCount; }
    public void setAvailableCount(int availableCount) { this.availableCount = availableCount; }

    public BigDecimal getEstimatedTotal() { return estimatedTotal; }
    public void setEstimatedTotal(BigDecimal estimatedTotal) { this.estimatedTotal = estimatedTotal; }

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

