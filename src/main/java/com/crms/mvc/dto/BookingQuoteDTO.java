package com.crms.mvc.dto;

import java.math.BigDecimal;

public record BookingQuoteDTO(BigDecimal totalPrice, BigDecimal basePrice, BigDecimal discount,
                              BigDecimal depositAmount, BigDecimal depositPercent, BigDecimal remainingAmount, String message, boolean valid) {
    public static BookingQuoteDTO from(PriceQuoteDTO quote) {
        return new BookingQuoteDTO(quote.getTotalPrice(), quote.getBaseRentalPrice(), quote.getDiscountAmount(),
                quote.getDepositAmount(), quote.getDepositRate().multiply(BigDecimal.valueOf(100)), quote.getRemainingAmount(),
                "Báo giá hiện tại; giá và lượt sử dụng mã được kiểm tra lại khi đặt xe.", true);
    }

    public static BookingQuoteDTO invalid(String message) {
        return new BookingQuoteDTO(null, null, null, null, null, null, message, false);
    }
}
