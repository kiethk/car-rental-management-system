package com.crms.mvc.service;

import com.crms.mvc.dto.VehicleInspectionFormDTO;
import com.crms.mvc.entity.VehicleInspection;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VehicleInspectionService {
    List<VehicleInspection> findAll();

    Optional<VehicleInspection> findById(Long id);

    VehicleInspection schedule(VehicleInspectionFormDTO dto);

    void startInspection(Long inspectionId);

    void recordResult(Long inspectionId, String status, String notes, LocalDate newRegistrationExpiry);

    List<VehicleInspection> findByVehicleId(Long vehicleId);
}
