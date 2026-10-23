package com.crms.mvc.service;

import com.crms.mvc.entity.BookingFee;
import java.math.BigDecimal;
import java.util.List;

public interface BookingFeeService {
    BookingFee recordFee(Long bookingId, String feeType, BigDecimal amount, String description, Long issueId);
    List<BookingFee> getFeesByBooking(Long bookingId);
    BigDecimal calculateTotalAdditionalFees(Long bookingId);
}
