package com.crms.mvc.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * A traffic fine (phạt nguội) the police sent about a rental. It only tells the customer which rental it belongs
 * to: no amount, no payment. The police contact the customer directly. Not an Issue, which always carries a fee.
 */
@Entity
@Table(name = "traffic_fine_notices")
public class TrafficFineNotice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "traffic_fine_notice_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    /** The plate printed on the police notice. */
    @Column(name = "plate", nullable = false, length = 20)
    private String plate;

    @Column(name = "violation_at", nullable = false)
    private OffsetDateTime violationAt;

    /** Photo of the police notice; the file sits in a private folder and is served behind the staff login. */
    @Column(name = "image_file_name", nullable = false, length = 255)
    private String imageFileName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by", nullable = false)
    private Employee recordedBy;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }

    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }

    public OffsetDateTime getViolationAt() { return violationAt; }
    public void setViolationAt(OffsetDateTime violationAt) { this.violationAt = violationAt; }

    public String getImageFileName() { return imageFileName; }
    public void setImageFileName(String imageFileName) { this.imageFileName = imageFileName; }

    public Employee getRecordedBy() { return recordedBy; }
    public void setRecordedBy(Employee recordedBy) { this.recordedBy = recordedBy; }

    public OffsetDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(OffsetDateTime recordedAt) { this.recordedAt = recordedAt; }
}
