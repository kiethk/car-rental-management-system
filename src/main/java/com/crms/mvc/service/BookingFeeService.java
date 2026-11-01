package com.crms.mvc.service;

import com.crms.mvc.entity.BookingFee;
import com.crms.mvc.entity.Payment;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface BookingFeeService {
    BookingFee recordFee(Long bookingId, String feeType, BigDecimal amount, String description, Long issueId);
    List<BookingFee> getFeesByBooking(Long bookingId);

    /**
     * Issue fees ready to be collected: ISSUE_COMPENSATION fees of open issues that no payment has taken yet.
     */
    List<BookingFee> getSettleableIssueFees(Long bookingId);

    /** Records that a payment (final settlement or a later issue payment) took these fees. */
    void attachPayment(Collection<BookingFee> fees, Payment payment);

    /** Confirms all fees taken by this payment as paid, and transitions attached open issues to RESOLVED. */
    void confirmPayment(Payment payment);

    /** Gives back every fee a payment had taken (the payment failed or was abandoned), so they are owed again. */
    void detachPayment(Payment payment);
    BigDecimal calculateTotalAdditionalFees(Long bookingId);
}
