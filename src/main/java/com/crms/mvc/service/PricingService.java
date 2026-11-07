package com.crms.mvc.service;

import com.crms.mvc.dto.LateReturnDTO;
import com.crms.mvc.dto.PriceQuoteDTO;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;

public interface PricingService {
    PriceQuoteDTO calculatePrice(Temporal startTime, Temporal endTime, BigDecimal pricePerHour, BigDecimal depositRate, BigDecimal discountAmount);

    long calculateRentalHours(Temporal startTime, Temporal endTime);

    BigDecimal calculateLateFee(OffsetDateTime scheduledEndTime, OffsetDateTime actualReturnTime, BigDecimal pricePerHour, BigDecimal lateFeeMultiplier);

    /**
     * How late a car still out is at {@code now}, with the late fee it would pay if returned now, using the same rule
     * as calculateLateFee. Null while the scheduled return time has not passed.
     */
    LateReturnDTO describeLateReturn(OffsetDateTime scheduledEndTime, OffsetDateTime now, BigDecimal pricePerHour, BigDecimal lateFeeMultiplier);
}
