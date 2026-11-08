package com.crms.mvc.service.impl;

import com.crms.mvc.dto.RatingSummaryDTO;
import com.crms.mvc.dto.ReviewDTO;
import com.crms.mvc.dto.ReviewFormDTO;
import com.crms.mvc.dto.VehicleReviewSummaryDTO;
import com.crms.mvc.entity.Booking;
import com.crms.mvc.entity.Review;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.PaymentRepository;
import com.crms.mvc.repository.ReviewRepository;
import com.crms.mvc.service.ReviewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ReviewServiceImpl implements ReviewService {
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewRepository reviewRepository;

    public ReviewServiceImpl(BookingRepository bookingRepository, PaymentRepository paymentRepository,
                             ReviewRepository reviewRepository) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.reviewRepository = reviewRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEligibleForCustomer(Long bookingId, Long customerId) {
        Booking booking = findOwnedBooking(bookingId, customerId);
        return isFinanciallyComplete(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewDTO findForCustomer(Long bookingId, Long customerId) {
        Booking booking = findOwnedBooking(bookingId, customerId);
        if (!isFinanciallyComplete(booking)) {
            return null;
        }
        return reviewRepository.findByBookingId(booking.getId())
                .map(this::toDTO)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleReviewSummaryDTO findPublicReviewsForVehicleType(Long vehicleTypeId) {
        List<ReviewDTO> reviews = reviewRepository.findPublicReviewsByVehicleTypeId(vehicleTypeId);
        BigDecimal averageRating = reviews.isEmpty()
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(reviews.stream()
                        .mapToInt(ReviewDTO::getRating)
                        .average()
                        .orElse(0))
                .setScale(1, RoundingMode.HALF_UP);
        return new VehicleReviewSummaryDTO(averageRating, reviews);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, RatingSummaryDTO> ratingsByVehicleType() {
        Map<Long, RatingSummaryDTO> ratings = new HashMap<>();
        for (Object[] row : reviewRepository.summarizeRatingsByVehicleType()) {
            double average = BigDecimal.valueOf(((Number) row[1]).doubleValue())
                    .setScale(1, RoundingMode.HALF_UP).doubleValue();
            ratings.put(((Number) row[0]).longValue(), new RatingSummaryDTO(average, ((Number) row[2]).longValue()));
        }
        return ratings;
    }

    @Override
    public void submit(Long bookingId, Long customerId, ReviewFormDTO form) {
        Booking booking = findOwnedBooking(bookingId, customerId);
        if (!isFinanciallyComplete(booking)) {
            throw new IllegalStateException("Chỉ có thể đánh giá sau khi chuyến đi và giao dịch đã hoàn tất.");
        }
        if (reviewRepository.findByBookingId(bookingId).isPresent()) {
            throw new IllegalStateException("Đơn này đã được đánh giá.");
        }

        Review review = new Review();
        review.setBooking(booking);
        review.setRating(form.getRating());
        review.setComment(form.getComment() == null || form.getComment().isBlank()
                ? null
                : form.getComment().trim());
        review.setCreatedAt(OffsetDateTime.now());
        reviewRepository.save(review);
    }

    private boolean isFinanciallyComplete(Booking booking) {
        return "completed".equalsIgnoreCase(booking.getStatus())
                && !paymentRepository.existsByBookingIdAndStatus(booking.getId(), "pending");
    }

    private Booking findOwnedBooking(Long bookingId, Long customerId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt xe."));
        if (booking.getCustomer() == null || !customerId.equals(booking.getCustomer().getId())) {
            throw new IllegalArgumentException("Bạn không có quyền truy cập đơn đặt xe này.");
        }
        return booking;
    }

    private ReviewDTO toDTO(Review review) {
        ReviewDTO dto = new ReviewDTO();
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCreatedAt(review.getCreatedAt());
        return dto;
    }
}
