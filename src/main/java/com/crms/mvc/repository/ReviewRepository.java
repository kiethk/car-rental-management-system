package com.crms.mvc.repository;
import com.crms.mvc.dto.ReviewDTO;
import com.crms.mvc.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByBookingId(Long bookingId);

    @Query("""
            SELECT new com.crms.mvc.dto.ReviewDTO(r.rating, r.comment, r.createdAt)
            FROM Review r
            JOIN r.booking b
            JOIN b.slot s
            WHERE b.status = com.crms.mvc.enums.BookingStatus.COMPLETED
              AND s.vehicleType.id = :vehicleTypeId
              AND NOT EXISTS (
                  SELECT p.id FROM Payment p
                  WHERE p.booking = b AND p.status = com.crms.mvc.enums.PaymentStatus.PENDING
              )
            ORDER BY r.createdAt DESC
            """)
    List<ReviewDTO> findPublicReviewsByVehicleTypeId(@Param("vehicleTypeId") Long vehicleTypeId);

    /** One row per vehicle type that has at least one public review: [vehicleTypeId, average rating, review count]. */
    @Query("""
            SELECT s.vehicleType.id, AVG(r.rating), COUNT(r)
            FROM Review r
            JOIN r.booking b
            JOIN b.slot s
            WHERE b.status = com.crms.mvc.enums.BookingStatus.COMPLETED
              AND NOT EXISTS (
                  SELECT p.id FROM Payment p
                  WHERE p.booking = b AND p.status = com.crms.mvc.enums.PaymentStatus.PENDING
              )
            GROUP BY s.vehicleType.id
            """)
    List<Object[]> summarizeRatingsByVehicleType();
}
