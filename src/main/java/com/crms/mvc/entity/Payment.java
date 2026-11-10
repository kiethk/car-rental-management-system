package com.crms.mvc.entity;

import com.crms.mvc.enums.PaymentMethod;
import com.crms.mvc.enums.PaymentStatus;
import com.crms.mvc.enums.PaymentType;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "payments")
public class Payment {
    public static final String ISSUE_TXN_PREFIX = "I";
    /** How long an online payment may stay unpaid: the deposit of a new booking and an issue payment session. */
    public static final int PAYMENT_TIMEOUT_MINUTES = 15;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "transaction_code", length = 100)
    private String transactionCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 40)
    private PaymentType paymentType;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentStatus status;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }

    public String getTransactionCode() { return transactionCode; }
    public void setTransactionCode(String transactionCode) { this.transactionCode = transactionCode; }

    public PaymentType getPaymentType() { return paymentType; }
    public void setPaymentType(PaymentType paymentType) { this.paymentType = paymentType; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public OffsetDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(OffsetDateTime paidAt) { this.paidAt = paidAt; }

    public boolean isExpired() {
        if (transactionCode == null) return false;
        int lastDash = transactionCode.lastIndexOf('-');
        if (lastDash >= 0 && lastDash < transactionCode.length() - 1) {
            try {
                long createdMillis = Long.parseLong(transactionCode.substring(lastDash + 1));
                return System.currentTimeMillis() - createdMillis > PAYMENT_TIMEOUT_MINUTES * 60 * 1000L;
            } catch (NumberFormatException ignored) {}
        }
        return false;
    }
}
