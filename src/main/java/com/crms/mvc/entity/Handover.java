package com.crms.mvc.entity;

import com.crms.mvc.enums.HandoverType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * The record staff make when giving the vehicle to the customer (CHECK_IN) or taking it back (CHECK_OUT).
 * Not an Issue: it carries no fee. A booking has at most one of each type.
 */
@Entity
@Table(name = "handovers")
public class Handover {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "handover_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "handover_type", nullable = false, length = 20)
    private HandoverType type;

    @Column(name = "odometer", nullable = false)
    private Integer odometer;

    /** Recorded when the car is given out (CHECK_IN); null on the return record. */
    @Column(name = "fuel_percent")
    private Integer fuelPercent;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "license_image_url", length = 500)
    private String licenseImageUrl;

    @Column(name = "id_card_image_url", length = 500)
    private String idCardImageUrl;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    /** Set on the CHECK_OUT record once the settlement is done. */
    @Column(name = "settled_at")
    private OffsetDateTime settledAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }

    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }

    public HandoverType getType() { return type; }
    public void setType(HandoverType type) { this.type = type; }

    public Integer getOdometer() { return odometer; }
    public void setOdometer(Integer odometer) { this.odometer = odometer; }

    public Integer getFuelPercent() { return fuelPercent; }
    public void setFuelPercent(Integer fuelPercent) { this.fuelPercent = fuelPercent; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getLicenseImageUrl() { return licenseImageUrl; }
    public void setLicenseImageUrl(String licenseImageUrl) { this.licenseImageUrl = licenseImageUrl; }

    public String getIdCardImageUrl() { return idCardImageUrl; }
    public void setIdCardImageUrl(String idCardImageUrl) { this.idCardImageUrl = idCardImageUrl; }

    public OffsetDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(OffsetDateTime recordedAt) { this.recordedAt = recordedAt; }

    public OffsetDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(OffsetDateTime settledAt) { this.settledAt = settledAt; }

    public boolean isSettled() { return settledAt != null; }
}
