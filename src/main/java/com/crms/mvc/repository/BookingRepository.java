package com.crms.mvc.repository;

import com.crms.mvc.entity.Booking;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    @EntityGraph(attributePaths = {"slot.vehicleType", "vehicle.licensePlate", "customer", "promotion", "vehicle.vehicleType"})
    Optional<Booking> findById(Long id);

    @EntityGraph(attributePaths = {"slot.vehicleType", "vehicle.licensePlate", "customer"})
    List<Booking> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    @EntityGraph(attributePaths = {"slot.vehicleType", "vehicle.licensePlate", "customer"})
    List<Booking> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"slot.vehicleType", "vehicle.licensePlate", "customer"})
    List<Booking> findByStatusOrderByCreatedAtDesc(String status);

    @Query("SELECT b.vehicle.id FROM Booking b WHERE b.vehicle IS NOT NULL " +
           "AND b.status IN ('confirmed', 'active') " +
           "AND b.pickupTime < :endTime AND b.scheduledEndTime > :startTime")
    List<Long> findBookedVehicleIdsInTimeRange(@Param("startTime") OffsetDateTime startTime,
                                              @Param("endTime") OffsetDateTime endTime);
}
