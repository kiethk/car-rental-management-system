package com.crms.mvc.dto;

import java.math.BigDecimal;
import java.util.List;

public class VehicleReviewSummaryDTO {
    private final BigDecimal averageRating;
    private final int reviewCount;
    private final List<ReviewDTO> reviews;

    public VehicleReviewSummaryDTO(BigDecimal averageRating, List<ReviewDTO> reviews) {
        this.averageRating = averageRating;
        this.reviewCount = reviews.size();
        this.reviews = List.copyOf(reviews);
    }

    public BigDecimal getAverageRating() { return averageRating; }
    public int getReviewCount() { return reviewCount; }
    public List<ReviewDTO> getReviews() { return reviews; }
}
