package com.crms.mvc.service;

import com.crms.mvc.dto.ReviewDTO;
import com.crms.mvc.dto.RatingSummaryDTO;
import com.crms.mvc.dto.ReviewFormDTO;
import com.crms.mvc.dto.VehicleReviewSummaryDTO;

import java.util.Map;

public interface ReviewService {
    boolean isEligibleForCustomer(Long bookingId, Long customerId);

    ReviewDTO findForCustomer(Long bookingId, Long customerId);

    VehicleReviewSummaryDTO findPublicReviewsForVehicleType(Long vehicleTypeId);

    /** Rating summary of every vehicle type that has public reviews, keyed by vehicle type id. */
    Map<Long, RatingSummaryDTO> ratingsByVehicleType();

    void submit(Long bookingId, Long customerId, ReviewFormDTO form);
}
