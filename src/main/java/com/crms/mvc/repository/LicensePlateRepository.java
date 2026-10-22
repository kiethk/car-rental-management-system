package com.crms.mvc.repository;

import com.crms.mvc.entity.LicensePlate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LicensePlateRepository extends JpaRepository<LicensePlate, Long> {
    List<LicensePlate> findAllByOrderByIdAsc();

    Optional<LicensePlate> findByPlateIgnoreCase(String plate);

    boolean existsByPlateIgnoreCase(String plate);

    boolean existsByPlateIgnoreCaseAndIdNot(String plate, Long id);

    @Query("SELECT lp FROM LicensePlate lp WHERE lp.id NOT IN (SELECT v.licensePlate.id FROM Vehicle v WHERE v.licensePlate IS NOT NULL) ORDER BY lp.plate ASC")
    List<LicensePlate> findUnassignedPlates();
}
