package com.crms.mvc.enums;

/** Why a payment row exists. Stored by name. */
public enum PaymentType {
    /** Paid online when the booking is created. */
    DEPOSIT("Đặt cọc"),
    /** The rest of the rent, collected by staff at handover. */
    RENTAL_BALANCE("Tiền thuê còn lại"),
    /** Settlement: only the fees still unpaid (late fee, return charges, open Issues). */
    FINAL("Quyết toán phí"),
    /** What a customer pays later for the fees of issues resolved after settlement. */
    ISSUE_COMPENSATION("Phí sự cố"),
    /** Money going back to the customer. */
    REFUND("Hoàn tiền");

    private final String label;

    PaymentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
