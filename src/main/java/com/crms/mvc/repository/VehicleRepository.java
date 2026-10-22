package com.crms.mvc.repository;

import com.crms.mvc.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findAllByOrderByIdAsc();

    List<Vehicle> findByVehicleTypeBrandContainingIgnoreCaseOrVehicleTypeNameContainingIgnoreCaseOrderByIdAsc(String brand, String name);

    long countByVehicleTypeId(Long vehicleTypeId);

    Optional<Vehicle> findByLicensePlateId(Long licensePlateId);

    boolean existsByLicensePlateId(Long licensePlateId);

    List<Vehicle> findByStatusOrderByIdAsc(String status);

    List<Vehicle> findByStatus(String status);

    @Query("SELECT v FROM Vehicle v JOIN FETCH v.vehicleType vt LEFT JOIN FETCH v.licensePlate lp WHERE v.id = :id")
    Optional<Vehicle> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT v FROM Vehicle v JOIN FETCH v.vehicleType vt LEFT JOIN FETCH v.licensePlate lp ORDER BY v.id ASC")
    List<Vehicle> findAllWithDetails();
}
