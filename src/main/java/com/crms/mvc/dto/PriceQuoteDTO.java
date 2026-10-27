package com.crms.mvc.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class PriceQuoteDTO implements Serializable {
    private long rentalHours;
    private BigDecimal pricePerHour;
    private BigDecimal baseRentalPrice;
    private BigDecimal discountAmount;
    private BigDecimal totalPrice;
    private BigDecimal depositRate;
    private BigDecimal depositAmount;
    private BigDecimal remainingAmount;

    public PriceQuoteDTO() {}

    public PriceQuoteDTO(long rentalHours, BigDecimal pricePerHour, BigDecimal discountAmount, BigDecimal depositRate) {
        this.rentalHours = rentalHours;
        this.pricePerHour = pricePerHour != null ? pricePerHour : BigDecimal.ZERO;
        this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        this.depositRate = depositRate != null ? depositRate : BigDecimal.ZERO;

        this.baseRentalPrice = this.pricePerHour.multiply(BigDecimal.valueOf(this.rentalHours)).setScale(0, RoundingMode.HALF_UP);
        
        BigDecimal discounted = this.baseRentalPrice.subtract(this.discountAmount);
        this.totalPrice = discounted.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : discounted.setScale(0, RoundingMode.HALF_UP);

        this.depositAmount = this.totalPrice.multiply(this.depositRate).setScale(0, RoundingMode.HALF_UP);
        this.remainingAmount = this.totalPrice.subtract(this.depositAmount).setScale(0, RoundingMode.HALF_UP);
    }

    public long getRentalHours() { return rentalHours; }
    public void setRentalHours(long rentalHours) { this.rentalHours = rentalHours; }

    public BigDecimal getPricePerHour() { return pricePerHour; }
    public void setPricePerHour(BigDecimal pricePerHour) { this.pricePerHour = pricePerHour; }

    public BigDecimal getBaseRentalPrice() { return baseRentalPrice; }
    public void setBaseRentalPrice(BigDecimal baseRentalPrice) { this.baseRentalPrice = baseRentalPrice; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public BigDecimal getDepositRate() { return depositRate; }
    public void setDepositRate(BigDecimal depositRate) { this.depositRate = depositRate; }

    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }

    public BigDecimal getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(BigDecimal remainingAmount) { this.remainingAmount = remainingAmount; }
}
