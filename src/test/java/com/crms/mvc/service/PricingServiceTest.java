package com.crms.mvc.service;

import com.crms.mvc.dto.PriceQuoteDTO;
import com.crms.mvc.service.impl.PricingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingServiceTest {

    private PricingService pricingService;

    @BeforeEach
    void setUp() {
        pricingService = new PricingServiceImpl();
    }

    @Test
    void testExactHours() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 25, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 25, 19, 0);
        long hours = pricingService.calculateRentalHours(start, end);
        assertEquals(10, hours);

        PriceQuoteDTO quote = pricingService.calculatePrice(start, end, new BigDecimal("100000"), new BigDecimal("0.30"), BigDecimal.ZERO);
        assertEquals(10, quote.getRentalHours());
        assertEquals(new BigDecimal("1000000"), quote.getTotalPrice());
        assertEquals(new BigDecimal("300000"), quote.getDepositAmount());
        assertEquals(new BigDecimal("700000"), quote.getRemainingAmount());
    }

    @Test
    void testPartialHourRoundsUp() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 25, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 25, 11, 15); // 2h 15m
        long hours = pricingService.calculateRentalHours(start, end);
        assertEquals(3, hours);
    }

    @Test
    void testLateFeeGracePeriod() {
        OffsetDateTime scheduled = OffsetDateTime.of(2026, 10, 25, 18, 0, 0, 0, ZoneOffset.ofHours(7));
        OffsetDateTime returnedWithinGrace = scheduled.plusMinutes(14);
        BigDecimal lateFee = pricingService.calculateLateFee(scheduled, returnedWithinGrace, new BigDecimal("100000"), new BigDecimal("2.00"));
        assertEquals(BigDecimal.ZERO, lateFee);

        OffsetDateTime returnedAfterGrace = scheduled.plusMinutes(25); // 25 mins late -> 1 hour charged
        BigDecimal fee = pricingService.calculateLateFee(scheduled, returnedAfterGrace, new BigDecimal("100000"), new BigDecimal("2.00"));
        assertEquals(new BigDecimal("200000"), fee);
    }
}
