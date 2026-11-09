package com.crms.mvc.dto;

import java.math.BigDecimal;
import java.util.List;

/** The reviews of one vehicle type with their average, for its detail page. */
public record VehicleReviewSummaryDTO(BigDecimal averageRating, List<ReviewDTO> reviews) {

    public VehicleReviewSummaryDTO {
        reviews = List.copyOf(reviews);
    }

    public int reviewCount() {
        return reviews.size();
    }
}
