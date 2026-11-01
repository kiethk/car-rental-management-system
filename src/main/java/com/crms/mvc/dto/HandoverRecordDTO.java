package com.crms.mvc.dto;

import java.time.OffsetDateTime;

public class HandoverRecordDTO {
    private Long id;
    private String type;
    private String subject;
    private String status;
    private String employeeName;
    private OffsetDateTime reportedAt;
    private boolean evidence;
    private boolean secondaryEvidence;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public OffsetDateTime getReportedAt() { return reportedAt; }
    public void setReportedAt(OffsetDateTime reportedAt) { this.reportedAt = reportedAt; }

    public boolean isEvidence() { return evidence; }
    public void setEvidence(boolean evidence) { this.evidence = evidence; }

    public boolean isSecondaryEvidence() { return secondaryEvidence; }
    public void setSecondaryEvidence(boolean secondaryEvidence) { this.secondaryEvidence = secondaryEvidence; }
}
