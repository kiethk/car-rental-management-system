package com.crms.mvc.service;

import com.crms.mvc.dto.PriceQuoteDTO;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;

public interface PricingService {
    PriceQuoteDTO calculatePrice(Temporal startTime, Temporal endTime, BigDecimal pricePerHour, BigDecimal depositRate, BigDecimal discountAmount);

    long calculateRentalHours(Temporal startTime, Temporal endTime);

    BigDecimal calculateLateFee(OffsetDateTime scheduledEndTime, OffsetDateTime actualReturnTime, BigDecimal pricePerHour, BigDecimal lateFeeMultiplier);
}
