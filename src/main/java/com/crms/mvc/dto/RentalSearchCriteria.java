package com.crms.mvc.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/**
 * The filters of the vehicle search page. "all" (or nothing) for brand and transmission, zero (or nothing) for seats
 * and no price limit mean "do not filter on this".
 */
public record RentalSearchCriteria(String keyword, String brand, Integer minSeats, String transmission, BigDecimal maxPrice) {

    /** The results that satisfy every filter, in their original order. */
    public List<VehicleTypeSearchResultDTO> narrow(List<VehicleTypeSearchResultDTO> results) {
        return results.stream().filter(this::matches).toList();
    }

    public boolean matches(VehicleTypeSearchResultDTO v) {
        return seatsFit(v) && priceFits(v) && keywordFits(v)
                && sameOrAll(brand, v.getBrand()) && sameOrAll(transmission, v.getTransmission());
    }

    private boolean seatsFit(VehicleTypeSearchResultDTO v) {
        return minSeats == null || minSeats == 0 || (v.getSeats() != null && v.getSeats() >= minSeats);
    }

    private boolean priceFits(VehicleTypeSearchResultDTO v) {
        return maxPrice == null || v.getPricePerHour().compareTo(maxPrice) <= 0;
    }

    private boolean keywordFits(VehicleTypeSearchResultDTO v) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String wanted = keyword.trim().toLowerCase(Locale.ROOT);
        return lower(v.getName()).contains(wanted) || lower(v.getBrand()).contains(wanted) || lower(v.getDisplayName()).contains(wanted);
    }

    private static boolean sameOrAll(String wanted, String actual) {
        if (wanted == null || wanted.isBlank() || wanted.equalsIgnoreCase("all")) {
            return true;
        }
        return actual != null && actual.equalsIgnoreCase(wanted.trim());
    }

    private static String lower(String text) {
        return text != null ? text.toLowerCase(Locale.ROOT) : "";
    }
}
