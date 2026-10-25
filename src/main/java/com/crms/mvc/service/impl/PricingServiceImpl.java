package com.crms.mvc.service.impl;

import com.crms.mvc.dto.PriceQuoteDTO;
import com.crms.mvc.service.PricingService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;

@Service
public class PricingServiceImpl implements PricingService {

    private static final int GRACE_PERIOD_MINUTES = 15;

    @Override
    public PriceQuoteDTO calculatePrice(Temporal startTime, Temporal endTime, BigDecimal pricePerHour, BigDecimal depositRate, BigDecimal discountAmount) {
        long rentalHours = calculateRentalHours(startTime, endTime);
        return new PriceQuoteDTO(rentalHours, pricePerHour, discountAmount, depositRate);
    }

    @Override
    public long calculateRentalHours(Temporal startTime, Temporal endTime) {
        if (startTime == null || endTime == null) {
            return 1;
        }
        long minutes = Duration.between(startTime, endTime).toMinutes();
        if (minutes <= 0) {
            return 1;
        }
        // Round up to the next full hour (e.g., 2h 5m -> 3h)
        return (minutes + 59) / 60;
    }

    @Override
    public BigDecimal calculateLateFee(OffsetDateTime scheduledEndTime, OffsetDateTime actualReturnTime, BigDecimal pricePerHour, BigDecimal lateFeeMultiplier) {
        if (scheduledEndTime == null || actualReturnTime == null || !actualReturnTime.isAfter(scheduledEndTime)) {
            return BigDecimal.ZERO;
        }

        long lateMinutes = Duration.between(scheduledEndTime, actualReturnTime).toMinutes();
        if (lateMinutes <= GRACE_PERIOD_MINUTES) {
            return BigDecimal.ZERO;
        }

        // Round up late hours
        long lateHours = (lateMinutes + 59) / 60;
        BigDecimal multiplier = (lateFeeMultiplier != null && lateFeeMultiplier.compareTo(BigDecimal.ZERO) > 0)
                ? lateFeeMultiplier
                : BigDecimal.ONE;

        return pricePerHour.multiply(BigDecimal.valueOf(lateHours)).multiply(multiplier).setScale(0, RoundingMode.HALF_UP);
    }
}
