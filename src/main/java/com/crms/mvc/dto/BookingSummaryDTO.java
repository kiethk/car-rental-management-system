package com.crms.mvc.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class BookingSummaryDTO implements Serializable {
    private Long id;
    private String vehicleTypeName;
    private String licensePlate;
    private OffsetDateTime pickupTime;
    private OffsetDateTime scheduledEndTime;
    private String status;
    private BigDecimal totalPrice;
    private BigDecimal depositAmount;
    private String customerName;
    private String imageUrl;
    private String vehicleCategory;
    private boolean hasPendingRefund;
    private BigDecimal refundAmount;

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getVehicleCategory() { return vehicleCategory; }
    public void setVehicleCategory(String vehicleCategory) { this.vehicleCategory = vehicleCategory; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getVehicleTypeName() { return vehicleTypeName; }
    public void setVehicleTypeName(String vehicleTypeName) { this.vehicleTypeName = vehicleTypeName; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public OffsetDateTime getPickupTime() { return pickupTime; }
    public void setPickupTime(OffsetDateTime pickupTime) { this.pickupTime = pickupTime; }

    public OffsetDateTime getScheduledEndTime() { return scheduledEndTime; }
    public void setScheduledEndTime(OffsetDateTime scheduledEndTime) { this.scheduledEndTime = scheduledEndTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public boolean isHasPendingRefund() { return hasPendingRefund; }
    public boolean getHasPendingRefund() { return hasPendingRefund; }
    public void setHasPendingRefund(boolean hasPendingRefund) { this.hasPendingRefund = hasPendingRefund; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
}

