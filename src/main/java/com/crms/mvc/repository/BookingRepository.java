package com.crms.mvc.repository;

import com.crms.mvc.entity.Booking;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    /**
     * Rentals of a vehicle that were under way at a moment, measured on the real handover times: from the actual
     * pickup to the actual return. A car not returned yet is still the customer's, even past its planned end.
     */
    @Query("SELECT b FROM Booking b WHERE b.vehicle.id = :vehicleId AND b.status IN ('active', 'completed') "
            + "AND COALESCE(b.actualPickupTime, b.pickupTime) <= :at "
            + "AND ((b.status = 'active' AND b.actualReturnTime IS NULL) OR :at <= b.actualReturnTime) "
            + "ORDER BY b.pickupTime DESC")
    List<Booking> findRentalsCovering(@Param("vehicleId") Long vehicleId, @Param("at") OffsetDateTime at);

    @EntityGraph(attributePaths = {"slot.vehicleType", "vehicle.currentPlates", "customer", "promotion", "vehicle.vehicleType", "employee"})
    Optional<Booking> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"slot.vehicleType", "vehicle.currentPlates", "customer", "employee"})
    List<Booking> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    @EntityGraph(attributePaths = {"slot.vehicleType", "vehicle.currentPlates", "customer", "employee"})
    List<Booking> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"slot.vehicleType", "vehicle.currentPlates", "customer", "employee"})
    List<Booking> findByStatusOrderByCreatedAtDesc(String status);

    @EntityGraph(attributePaths = {"slot.vehicleType", "vehicle.currentPlates", "customer", "employee"})
    @Query("SELECT DISTINCT b FROM Booking b, Payment p WHERE p.booking = b AND p.paymentType = 'REFUND' AND p.status = 'pending' ORDER BY b.createdAt DESC")
    List<Booking> findBookingsWithPendingRefund();

    @Query("SELECT b FROM Booking b WHERE b.vehicle.id = :vehicleId " +
           "AND b.id != COALESCE(:excludeBookingId, -1L) " +
           "AND b.actualReturnTime IS NOT NULL ORDER BY b.actualReturnTime DESC")
    List<Booking> findRecentReturnsByVehicle(@Param("vehicleId") Long vehicleId,
                                            @Param("excludeBookingId") Long excludeBookingId);

    long countByVehicleId(Long vehicleId);

    boolean existsByPromotionId(Long promotionId);

    /**
     * Vehicles taken during [startTime, endTime). A rental that is active but not returned yet keeps its vehicle
     * until at least {@code now}, even past its scheduled end, because the car has not physically come back.
     */
    @Query("SELECT b.vehicle.id FROM Booking b WHERE b.vehicle IS NOT NULL " +
           "AND (b.status IN ('confirmed', 'active') OR (b.status = 'pending_payment' AND b.createdAt > :cutoffTime)) " +
           "AND b.pickupTime < :endTime " +
           "AND (b.scheduledEndTime > :startTime " +
           "     OR (b.status = 'active' AND b.actualReturnTime IS NULL AND :now > :startTime))")
    List<Long> findBookedVehicleIdsInTimeRange(@Param("startTime") OffsetDateTime startTime,
                                              @Param("endTime") OffsetDateTime endTime,
                                              @Param("cutoffTime") OffsetDateTime cutoffTime,
                                              @Param("now") OffsetDateTime now);

    @Query("SELECT b FROM Booking b WHERE b.customer.id = :customerId " +
           "AND b.status = 'pending_payment' " +
           "AND b.pickupTime < :endTime AND b.scheduledEndTime > :startTime")
    List<Booking> findPendingBookingsByCustomerAndTimeRange(@Param("customerId") Long customerId,
                                                           @Param("startTime") OffsetDateTime startTime,
                                                           @Param("endTime") OffsetDateTime endTime);

    @Modifying
    @Query("UPDATE Booking b SET b.status = 'cancelled', b.cancelledAt = CURRENT_TIMESTAMP " +
           "WHERE b.status = 'pending_payment' AND b.createdAt < :cutoffTime")
    int cancelExpiredPendingBookings(@Param("cutoffTime") OffsetDateTime cutoffTime);
}
