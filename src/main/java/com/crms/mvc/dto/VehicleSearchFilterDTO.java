package com.crms.mvc.dto;

import java.math.BigDecimal;

public class VehicleSearchFilterDTO {
    private String keyword;
    private String brand;
    private String category;
    private Integer seats;
    private String transmission;
    private String fuelType;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String sortBy = "price_asc";
    private String status = "available";

    public VehicleSearchFilterDTO() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

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

    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }

    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
}
