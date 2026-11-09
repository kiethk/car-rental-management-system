package com.crms.mvc.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "issues")
public class Issue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "issue_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id")
    private Slot slot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    /** The employee who filed the report; never changes afterwards. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_type", nullable = false, length = 30)
    private IssueType issueType;

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Column(name = "description")
    private String description;

    @Convert(converter = IssueStatusConverter.class)
    @Column(name = "status", nullable = false, length = 30)
    private IssueStatus status = IssueStatus.OPEN;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "evidence_url", length = 500)
    private String evidenceUrl;

    /** When the customer was emailed about this issue; null for issues recorded before emails went out at once. */
    @Column(name = "notified_at")
    private OffsetDateTime notifiedAt;

    @Column(name = "reported_at", nullable = false)
    private OffsetDateTime reportedAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "resolution_note", length = 1000)
    private String resolutionNote;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @OneToMany(mappedBy = "issue")
    private List<IssueEvidence> evidences = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }

    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }

    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }

    public IssueType getIssueType() { return issueType; }
    public void setIssueType(IssueType issueType) { this.issueType = issueType; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public IssueStatus getStatus() { return status; }
    public void setStatus(IssueStatus status) { this.status = status; }

    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(OffsetDateTime occurredAt) { this.occurredAt = occurredAt; }

    public OffsetDateTime getNotifiedAt() { return notifiedAt; }
    public void setNotifiedAt(OffsetDateTime notifiedAt) { this.notifiedAt = notifiedAt; }

    public OffsetDateTime getReportedAt() { return reportedAt; }
    public void setReportedAt(OffsetDateTime reportedAt) { this.reportedAt = reportedAt; }

    public OffsetDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(OffsetDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String resolutionNote) { this.resolutionNote = resolutionNote; }

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }

    public List<IssueEvidence> getEvidences() { return evidences; }
    public void setEvidences(List<IssueEvidence> evidences) { this.evidences = evidences; }

    public Slot getSlot() { return slot; }
    public void setSlot(Slot slot) { this.slot = slot; }

    public String getEvidenceUrl() { return evidenceUrl; }
    public void setEvidenceUrl(String evidenceUrl) { this.evidenceUrl = evidenceUrl; }

    public void setIssueType(String typeStr) {
        if (typeStr == null) {
            this.issueType = null;
            return;
        }
        try {
            this.issueType = IssueType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            this.issueType = IssueType.OTHER;
        }
    }

    public void setStatus(String statusStr) {
        if (statusStr == null) {
            this.status = IssueStatus.OPEN;
            return;
        }
        for (IssueStatus s : IssueStatus.values()) {
            if (s.name().equalsIgnoreCase(statusStr) || s.dbValue().equalsIgnoreCase(statusStr)) {
                this.status = s;
                return;
            }
        }
        this.status = IssueStatus.OPEN;
    }

    @PrePersist
    public void prePersist() {
        if (occurredAt == null) {
            occurredAt = OffsetDateTime.now();
        }
        if (reportedAt == null) {
            reportedAt = OffsetDateTime.now();
        }
    }
}