package com.crms.mvc.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "license_documents")
public class LicenseDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "license_document_id")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "license_number", nullable = false, length = 50)
    private String licenseNumber;

    @Column(name = "license_class", nullable = false, length = 20)
    private String licenseClass;

    @Column(name = "license_expiry", nullable = false)
    private LocalDate licenseExpiry;

    @Column(name = "license_image_url", nullable = false, length = 500)
    private String licenseImageUrl;

    @Column(name = "submitted_at", nullable = false)
    private OffsetDateTime submittedAt;

    public LicenseDocument() {
    }

    public LicenseDocument(Customer customer,
                           String licenseNumber,
                           String licenseClass,
                           LocalDate licenseExpiry,
                           String licenseImageUrl,
                           OffsetDateTime submittedAt) {
        this.customer = customer;
        this.licenseNumber = licenseNumber;
        this.licenseClass = licenseClass;
        this.licenseExpiry = licenseExpiry;
        this.licenseImageUrl = licenseImageUrl;
        this.submittedAt = submittedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public String getLicenseClass() { return licenseClass; }
    public void setLicenseClass(String licenseClass) { this.licenseClass = licenseClass; }

    public LocalDate getLicenseExpiry() { return licenseExpiry; }
    public void setLicenseExpiry(LocalDate licenseExpiry) { this.licenseExpiry = licenseExpiry; }

    public String getLicenseImageUrl() { return licenseImageUrl; }
    public void setLicenseImageUrl(String licenseImageUrl) { this.licenseImageUrl = licenseImageUrl; }

    public OffsetDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(OffsetDateTime submittedAt) { this.submittedAt = submittedAt; }
}
