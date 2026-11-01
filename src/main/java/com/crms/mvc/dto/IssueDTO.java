package com.crms.mvc.dto;

import com.crms.mvc.entity.IssueStatus;
import com.crms.mvc.entity.IssueType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/** One Issue as the admin and customer pages show it, with its fee and photos already resolved. */
public class IssueDTO {
    private Long id;
    private Long bookingId;
    private String customerName;
    private Long vehicleId;
    private String licensePlate;
    private String vehicleName;
    private String reporterName;
    private IssueType issueType;
    private IssueStatus status;
    private String subject;
    private String description;
    private OffsetDateTime occurredAt;
    private OffsetDateTime reportedAt;
    private OffsetDateTime resolvedAt;
    private String resolutionNote;
    private String cancelReason;

    /** The Issue's fee; null when the Issue has none. */
    private BigDecimal feeAmount;
    /** True once a payment (paid or still pending) has taken over the fee, which locks the Issue. */
    private boolean feeLocked;
    private boolean feePaid;
    private List<EvidenceDTO> evidences = new ArrayList<>();

    /** One photo of the Issue; the file itself is served by a controller that checks access. */
    public record EvidenceDTO(Long id, String fileName, OffsetDateTime uploadedAt) {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public String getVehicleName() { return vehicleName; }
    public void setVehicleName(String vehicleName) { this.vehicleName = vehicleName; }

    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }

    public IssueType getIssueType() { return issueType; }
    public void setIssueType(IssueType issueType) { this.issueType = issueType; }

    public IssueStatus getStatus() { return status; }
    public void setStatus(IssueStatus status) { this.status = status; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(OffsetDateTime occurredAt) { this.occurredAt = occurredAt; }

    public OffsetDateTime getReportedAt() { return reportedAt; }
    public void setReportedAt(OffsetDateTime reportedAt) { this.reportedAt = reportedAt; }

    public OffsetDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(OffsetDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String resolutionNote) { this.resolutionNote = resolutionNote; }

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }

    public BigDecimal getFeeAmount() { return feeAmount; }
    public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }

    public boolean isFeeLocked() { return feeLocked; }
    public void setFeeLocked(boolean feeLocked) { this.feeLocked = feeLocked; }

    public boolean isFeePaid() { return feePaid; }
    public void setFeePaid(boolean feePaid) { this.feePaid = feePaid; }

    public List<EvidenceDTO> getEvidences() { return evidences; }
    public void setEvidences(List<EvidenceDTO> evidences) { this.evidences = evidences; }

    public boolean isOpen() { return status == IssueStatus.OPEN; }

    /** Photos can be added until the issue is cancelled or a payment has taken its fee. */
    public boolean isPhotosAddable() { return status != IssueStatus.CANCELLED && !feeLocked; }

    /** Photos can only be removed while the issue is open. */
    public boolean isPhotosRemovable() { return isOpen(); }
}
