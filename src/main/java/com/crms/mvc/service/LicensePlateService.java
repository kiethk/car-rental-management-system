package com.crms.mvc.service;

import com.crms.mvc.dto.LicensePlateOverviewDTO;
import com.crms.mvc.entity.LicensePlate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LicensePlateService {
    List<LicensePlateOverviewDTO> findAllOverviews();

    Optional<LicensePlate> findById(Long id);

    LicensePlate update(Long id, String province, LocalDate registrationExpiry);

    List<LicensePlate> findUnassignedPlates();

    boolean isPlateExists(String plate, Long excludeId);
}
