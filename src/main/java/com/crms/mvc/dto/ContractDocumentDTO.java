package com.crms.mvc.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Everything printed on the rental contract PDF, collected once so the document never reads entities directly.
 */
public class ContractDocumentDTO {
    private String contractNumber;
    private Long bookingId;
    private OffsetDateTime issuedAt;

    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String licenseNumber;
    private String licenseClass;
    private LocalDate licenseExpiry;

    private String vehicleName;
    private String vehicleCategory;
    private Integer seats;
    private String transmission;
    private String fuelType;
    private String licensePlate;
    private String vehicleColor;
    private Integer vehicleYear;

    private OffsetDateTime pickupTime;
    private OffsetDateTime scheduledEndTime;
    private long rentalHours;

    private BigDecimal pricePerHour;
    private BigDecimal rentalPrice;
    private BigDecimal discountAmount;
    private String promotionCode;
    private BigDecimal totalPrice;
    private BigDecimal depositAmount;
    private BigDecimal remainingAmount;
    private String depositTransactionCode;
    private OffsetDateTime depositPaidAt;

    private OffsetDateTime acceptedAt;
    private String acceptedIp;
    private ContractTemplateDTO terms;

    public String getContractNumber() { return contractNumber; }
    public void setContractNumber(String contractNumber) { this.contractNumber = contractNumber; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public OffsetDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(OffsetDateTime issuedAt) { this.issuedAt = issuedAt; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public String getLicenseClass() { return licenseClass; }
    public void setLicenseClass(String licenseClass) { this.licenseClass = licenseClass; }

    public LocalDate getLicenseExpiry() { return licenseExpiry; }
    public void setLicenseExpiry(LocalDate licenseExpiry) { this.licenseExpiry = licenseExpiry; }

    public String getVehicleName() { return vehicleName; }
    public void setVehicleName(String vehicleName) { this.vehicleName = vehicleName; }

    public String getVehicleCategory() { return vehicleCategory; }
    public void setVehicleCategory(String vehicleCategory) { this.vehicleCategory = vehicleCategory; }

    public Integer getSeats() { return seats; }
    public void setSeats(Integer seats) { this.seats = seats; }

    public String getTransmission() { return transmission; }
    public void setTransmission(String transmission) { this.transmission = transmission; }

    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public String getVehicleColor() { return vehicleColor; }
    public void setVehicleColor(String vehicleColor) { this.vehicleColor = vehicleColor; }

    public Integer getVehicleYear() { return vehicleYear; }
    public void setVehicleYear(Integer vehicleYear) { this.vehicleYear = vehicleYear; }

    public OffsetDateTime getPickupTime() { return pickupTime; }
    public void setPickupTime(OffsetDateTime pickupTime) { this.pickupTime = pickupTime; }

    public OffsetDateTime getScheduledEndTime() { return scheduledEndTime; }
    public void setScheduledEndTime(OffsetDateTime scheduledEndTime) { this.scheduledEndTime = scheduledEndTime; }

    public long getRentalHours() { return rentalHours; }
    public void setRentalHours(long rentalHours) { this.rentalHours = rentalHours; }

    public BigDecimal getPricePerHour() { return pricePerHour; }
    public void setPricePerHour(BigDecimal pricePerHour) { this.pricePerHour = pricePerHour; }

    public BigDecimal getRentalPrice() { return rentalPrice; }
    public void setRentalPrice(BigDecimal rentalPrice) { this.rentalPrice = rentalPrice; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

    public String getPromotionCode() { return promotionCode; }
    public void setPromotionCode(String promotionCode) { this.promotionCode = promotionCode; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }

    public BigDecimal getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(BigDecimal remainingAmount) { this.remainingAmount = remainingAmount; }

    public String getDepositTransactionCode() { return depositTransactionCode; }
    public void setDepositTransactionCode(String depositTransactionCode) { this.depositTransactionCode = depositTransactionCode; }

    public OffsetDateTime getDepositPaidAt() { return depositPaidAt; }
    public void setDepositPaidAt(OffsetDateTime depositPaidAt) { this.depositPaidAt = depositPaidAt; }

    public OffsetDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(OffsetDateTime acceptedAt) { this.acceptedAt = acceptedAt; }

    public String getAcceptedIp() { return acceptedIp; }
    public void setAcceptedIp(String acceptedIp) { this.acceptedIp = acceptedIp; }

    public ContractTemplateDTO getTerms() { return terms; }
    public void setTerms(ContractTemplateDTO terms) { this.terms = terms; }
}
