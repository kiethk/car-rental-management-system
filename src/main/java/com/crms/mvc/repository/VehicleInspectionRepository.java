package com.crms.mvc.repository;

import com.crms.mvc.enums.InspectionStatus;
import com.crms.mvc.entity.VehicleInspection;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VehicleInspectionRepository extends JpaRepository<VehicleInspection, Long> {
    @Query("SELECT vi FROM VehicleInspection vi JOIN FETCH vi.vehicle v JOIN FETCH v.vehicleType vt LEFT JOIN FETCH v.currentPlates lp ORDER BY vi.scheduledDate DESC")
    List<VehicleInspection> findAllByOrderByScheduledDateDesc();

    @Query("SELECT vi FROM VehicleInspection vi JOIN FETCH vi.vehicle v JOIN FETCH v.vehicleType vt LEFT JOIN FETCH v.currentPlates lp WHERE vi.id = :id")
    Optional<VehicleInspection> findByIdWithDetails(@Param("id") Long id);

    List<VehicleInspection> findByVehicleIdOrderByScheduledDateDesc(Long vehicleId);

    List<VehicleInspection> findByStatusOrderByScheduledDateDesc(InspectionStatus status);

    long countByVehicleId(Long vehicleId);
}
