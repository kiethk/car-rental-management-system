package com.crms.mvc.repository;

import com.crms.mvc.entity.LicensePlate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface LicensePlateRepository extends JpaRepository<LicensePlate, Long> {
    List<LicensePlate> findByVehicleIdOrderByValidFromDesc(Long vehicleId);

    Optional<LicensePlate> findByVehicleIdAndValidToIsNull(Long vehicleId);

    boolean existsByPlateIgnoreCase(String plate);

    Optional<LicensePlate> findByPlateIgnoreCase(String plate);

    /** The plate a vehicle carried at a given moment (plates have a history). */
    @Query("SELECT lp FROM LicensePlate lp WHERE lp.vehicle.id = :vehicleId AND lp.validFrom <= :at "
            + "AND (lp.validTo IS NULL OR lp.validTo > :at)")
    Optional<LicensePlate> findCarriedAt(@Param("vehicleId") Long vehicleId, @Param("at") OffsetDateTime at);
}
