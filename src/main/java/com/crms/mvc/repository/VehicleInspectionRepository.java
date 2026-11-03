package com.crms.mvc.repository;

import com.crms.mvc.entity.VehicleInspection;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VehicleInspectionRepository extends JpaRepository<VehicleInspection, Long> {
    @Query("SELECT vi FROM VehicleInspection vi JOIN FETCH vi.vehicle v JOIN FETCH v.vehicleType vt LEFT JOIN FETCH v.licensePlate lp ORDER BY vi.scheduledAt DESC")
    List<VehicleInspection> findAllByOrderByScheduledAtDesc();

    @Query("SELECT vi FROM VehicleInspection vi JOIN FETCH vi.vehicle v JOIN FETCH v.vehicleType vt LEFT JOIN FETCH v.licensePlate lp WHERE vi.id = :id")
    Optional<VehicleInspection> findByIdWithDetails(@Param("id") Long id);

    List<VehicleInspection> findByVehicleIdOrderByScheduledAtDesc(Long vehicleId);

    List<VehicleInspection> findByStatusOrderByScheduledAtDesc(String status);

    long countByVehicleId(Long vehicleId);
}
