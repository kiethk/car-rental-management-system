package com.crms.mvc.controller;

import com.crms.mvc.dto.RentalSearchCriteria;
import com.crms.mvc.dto.RentalWindow;
import com.crms.mvc.util.RentalWindowParser;

import java.math.BigDecimal;

/**
 * The query string of the vehicle search page. The window comes in under several spellings (see
 * {@link RentalWindowParser}) and is kept as typed; the filters have the defaults the page shows.
 */
public class RentalSearchParams {
    private String startDate;
    private String endDate;
    private String date;
    private String startTime;
    private String endTime;

    private String keyword;
    private String brand = "all";
    private String category = "all";
    private Integer seats = 0;
    private String transmission = "all";
    private BigDecimal maxPrice;
    private String sortBy = "price";

    public RentalWindow window() {
        return RentalWindowParser.parse(startDate, endDate, date, startTime, endTime);
    }

    public RentalSearchCriteria criteria() {
        return new RentalSearchCriteria(keyword, brand, seats, transmission, maxPrice);
    }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = orDefault(brand, "all"); }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = orDefault(category, "all"); }

    public Integer getSeats() { return seats; }
    public void setSeats(Integer seats) { this.seats = seats != null ? seats : 0; }

    public String getTransmission() { return transmission; }
    public void setTransmission(String transmission) { this.transmission = orDefault(transmission, "all"); }

    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = orDefault(sortBy, "price"); }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
