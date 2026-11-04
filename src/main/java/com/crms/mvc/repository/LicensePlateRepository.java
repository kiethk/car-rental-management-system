package com.crms.mvc.repository;

import com.crms.mvc.entity.LicensePlate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LicensePlateRepository extends JpaRepository<LicensePlate, Long> {
    List<LicensePlate> findByVehicleIdOrderByValidFromDesc(Long vehicleId);

    Optional<LicensePlate> findByVehicleIdAndValidToIsNull(Long vehicleId);

    boolean existsByPlateIgnoreCase(String plate);
}
