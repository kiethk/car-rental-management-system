package com.crms.mvc.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "contracts")
public class Contract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contract_id")
    private Long id;

    @Column(name = "contract_number", nullable = false, unique = true, length = 50)
    private String contractNumber;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Column(name = "signed_at")
    private OffsetDateTime signedAt;

    @Column(name = "signature_type", length = 30)
    private String signatureType = "DIGITAL_CHECK";

    @Column(name = "terms_snapshot", nullable = false, columnDefinition = "TEXT")
    private String termsSnapshot;

    @Column(name = "contract_status", nullable = false, length = 30)
    private String contractStatus = "DRAFT";

    @Column(name = "file_pdf_url", length = 500)
    private String filePdfUrl;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContractNumber() { return contractNumber; }
    public void setContractNumber(String contractNumber) { this.contractNumber = contractNumber; }

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }

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
