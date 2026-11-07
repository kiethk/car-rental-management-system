package com.crms.mvc.service;

import com.crms.mvc.dto.ReviewDTO;
import com.crms.mvc.dto.ReviewFormDTO;
import com.crms.mvc.dto.VehicleReviewSummaryDTO;

public interface ReviewService {
    boolean isEligibleForCustomer(Long bookingId, Long customerId);

    ReviewDTO findForCustomer(Long bookingId, Long customerId);

    VehicleReviewSummaryDTO findPublicReviewsForVehicleType(Long vehicleTypeId);

    void submit(Long bookingId, Long customerId, ReviewFormDTO form);
}
