package com.crms.mvc.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "payments")
public class Payment {
    /** What a customer pays later for the fees of issues resolved after settlement. */
    public static final String TYPE_ISSUE_COMPENSATION = "ISSUE_COMPENSATION";
    /** The rest of the rent, collected by staff at handover. */
    public static final String TYPE_RENTAL_BALANCE = "RENTAL_BALANCE";
    /** Settlement: only the fees still unpaid (late fee, return charges, open Issues). */
    public static final String TYPE_FINAL = "FINAL";
    public static final String TYPE_REFUND = "REFUND";
    public static final String TYPE_DEPOSIT = "DEPOSIT";

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_PAID = "paid";
    public static final String STATUS_FAILED = "failed";

    public static final String METHOD_VNPAY = "VNPAY";
    public static final String METHOD_BANK_TRANSFER = "BANK_TRANSFER";

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

    @Column(name = "payment_type", nullable = false, length = 40)
    private String paymentType;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_method", nullable = false, length = 30)
    private String paymentMethod = METHOD_VNPAY;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }

    public String getTransactionCode() { return transactionCode; }
    public void setTransactionCode(String transactionCode) { this.transactionCode = transactionCode; }

    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

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
