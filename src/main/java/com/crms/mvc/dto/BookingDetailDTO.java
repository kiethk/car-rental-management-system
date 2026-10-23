package com.crms.mvc.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public class BookingDetailDTO implements Serializable {
    private Long id;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String customerEmail;

    private Long vehicleTypeId;
    private String vehicleTypeName;
    private String vehicleCategory;
    private Integer seats;
    private String imageUrl;

    private Long vehicleId;
    private String licensePlate;
    private String vehicleColor;

    private Long slotId;
    private OffsetDateTime pickupTime;
    private OffsetDateTime scheduledEndTime;
    private OffsetDateTime actualReturnTime;

    private BigDecimal pricePerHour;
    private BigDecimal depositAmount;
    private BigDecimal discountAmount;
    private BigDecimal lateFee;
    private BigDecimal totalPrice;
    private String promotionCode;

    private String status;
    private OffsetDateTime createdAt;

    private List<PaymentDTO> payments;
    private ContractDTO contract;
    private List<BookingFeeDTO> fees;

    public static class PaymentDTO implements Serializable {
        private Long id;
        private String transactionCode;
        private String paymentType;
        private BigDecimal amount;
        private String paymentMethod;
        private String status;
        private OffsetDateTime paidAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTransactionCode() { return transactionCode; }
        public void setTransactionCode(String transactionCode) { this.transactionCode = transactionCode; }
        public String getPaymentType() { return paymentType; }
        public void setPaymentType(String paymentType) { this.paymentType = paymentType; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public OffsetDateTime getPaidAt() { return paidAt; }
        public void setPaidAt(OffsetDateTime paidAt) { this.paidAt = paidAt; }
    }

    public static class ContractDTO implements Serializable {
        private Long id;
        private String contractNumber;
        private OffsetDateTime signedAt;
        private String signatureType;
        private String termsSnapshot;
        private String contractStatus;
        private String filePdfUrl;
        private OffsetDateTime createdAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getContractNumber() { return contractNumber; }
        public void setContractNumber(String contractNumber) { this.contractNumber = contractNumber; }
        public OffsetDateTime getSignedAt() { return signedAt; }
        public void setSignedAt(OffsetDateTime signedAt) { this.signedAt = signedAt; }
        public String getSignatureType() { return signatureType; }
        public void setSignatureType(String signatureType) { this.signatureType = signatureType; }
        public String getTermsSnapshot() { return termsSnapshot; }
        public void setTermsSnapshot(String termsSnapshot) { this.termsSnapshot = termsSnapshot; }
        public String getContractStatus() { return contractStatus; }
        public void setContractStatus(String contractStatus) { this.contractStatus = contractStatus; }
        public String getFilePdfUrl() { return filePdfUrl; }
        public void setFilePdfUrl(String filePdfUrl) { this.filePdfUrl = filePdfUrl; }
        public OffsetDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    }

    public static class BookingFeeDTO implements Serializable {
        private Long id;
        private String feeType;
        private BigDecimal amount;
        private String description;
        private Long issueId;
        private OffsetDateTime createdAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getFeeType() { return feeType; }
        public void setFeeType(String feeType) { this.feeType = feeType; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Long getIssueId() { return issueId; }
        public void setIssueId(Long issueId) { this.issueId = issueId; }
        public OffsetDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public Long getVehicleTypeId() { return vehicleTypeId; }
    public void setVehicleTypeId(Long vehicleTypeId) { this.vehicleTypeId = vehicleTypeId; }
    public String getVehicleTypeName() { return vehicleTypeName; }
    public void setVehicleTypeName(String vehicleTypeName) { this.vehicleTypeName = vehicleTypeName; }
    public String getVehicleCategory() { return vehicleCategory; }
    public void setVehicleCategory(String vehicleCategory) { this.vehicleCategory = vehicleCategory; }
    public Integer getSeats() { return seats; }
    public void setSeats(Integer seats) { this.seats = seats; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }
    public String getVehicleColor() { return vehicleColor; }
    public void setVehicleColor(String vehicleColor) { this.vehicleColor = vehicleColor; }
    public Long getSlotId() { return slotId; }
    public void setSlotId(Long slotId) { this.slotId = slotId; }
    public OffsetDateTime getPickupTime() { return pickupTime; }
    public void setPickupTime(OffsetDateTime pickupTime) { this.pickupTime = pickupTime; }
    public OffsetDateTime getScheduledEndTime() { return scheduledEndTime; }
    public void setScheduledEndTime(OffsetDateTime scheduledEndTime) { this.scheduledEndTime = scheduledEndTime; }
    public OffsetDateTime getActualReturnTime() { return actualReturnTime; }
    public void setActualReturnTime(OffsetDateTime actualReturnTime) { this.actualReturnTime = actualReturnTime; }
    public BigDecimal getPricePerHour() { return pricePerHour; }
    public void setPricePerHour(BigDecimal pricePerHour) { this.pricePerHour = pricePerHour; }
    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getLateFee() { return lateFee; }
    public void setLateFee(BigDecimal lateFee) { this.lateFee = lateFee; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    public String getPromotionCode() { return promotionCode; }
    public void setPromotionCode(String promotionCode) { this.promotionCode = promotionCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public List<PaymentDTO> getPayments() { return payments; }
    public void setPayments(List<PaymentDTO> payments) { this.payments = payments; }
    public ContractDTO getContract() { return contract; }
    public void setContract(ContractDTO contract) { this.contract = contract; }
    public List<BookingFeeDTO> getFees() { return fees; }
    public void setFees(List<BookingFeeDTO> fees) { this.fees = fees; }
}
