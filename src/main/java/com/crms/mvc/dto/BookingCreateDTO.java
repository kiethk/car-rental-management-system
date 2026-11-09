package com.crms.mvc.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import java.io.Serializable;
import java.time.LocalDateTime;

public class BookingCreateDTO implements Serializable {
    @NotNull(message = "Vui lòng chọn mẫu xe")
    private Long vehicleTypeId;

    @NotNull(message = "Vui lòng chọn giờ nhận xe")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    @NotNull(message = "Vui lòng chọn giờ trả xe")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;

    private Long vehicleId; // Optional: preferred physical vehicle
    private String promotionCode;

    // Clickwrap: the customer must accept exactly the template version shown on the checkout page.
    @NotNull(message = "Thiếu phiên bản điều khoản thuê xe. Vui lòng tải lại trang.")
    private Long contractTemplateId;

    @AssertTrue(message = "Bạn cần đồng ý với điều khoản thuê xe để tiếp tục")
    private boolean acceptedTerms;

    public Long getVehicleTypeId() { return vehicleTypeId; }
    public void setVehicleTypeId(Long vehicleTypeId) { this.vehicleTypeId = vehicleTypeId; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }

    public String getPromotionCode() { return promotionCode; }
    public void setPromotionCode(String promotionCode) { this.promotionCode = promotionCode; }

    public Long getContractTemplateId() { return contractTemplateId; }
    public void setContractTemplateId(Long contractTemplateId) { this.contractTemplateId = contractTemplateId; }

    public boolean isAcceptedTerms() { return acceptedTerms; }
    public void setAcceptedTerms(boolean acceptedTerms) { this.acceptedTerms = acceptedTerms; }
}
