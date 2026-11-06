package com.crms.mvc.dto;

import com.crms.mvc.entity.IssueType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** What staff type when recording or editing an Issue. The fee is optional; empty or 0 means no fee. */
public class IssueFormDTO {
    @NotNull(message = "Chưa chọn đơn thuê")
    private Long bookingId;

    @NotNull(message = "Vui lòng chọn loại sự cố")
    private IssueType issueType;

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
    private String subject;

    @Size(max = 4000, message = "Mô tả tối đa 4000 ký tự")
    private String description;

    /** When it happened, in Vietnam time (the form's datetime-local input has no zone). */
    @NotNull(message = "Vui lòng nhập thời điểm xảy ra")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime occurredAt;

    @NotNull(message = "Vui lòng nhập số tiền bồi thường")
    @DecimalMin(value = "1000", message = "Phí bồi thường tối thiểu là 1.000 VNĐ")
    private BigDecimal feeAmount;

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public IssueType getIssueType() { return issueType; }
    public void setIssueType(IssueType issueType) { this.issueType = issueType; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }

    public BigDecimal getFeeAmount() { return feeAmount; }
    public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }
}
