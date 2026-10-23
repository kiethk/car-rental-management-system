package com.crms.mvc.service.impl;

import com.crms.mvc.dto.VehicleInspectionFormDTO;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.entity.VehicleInspection;
import com.crms.mvc.repository.LicensePlateRepository;
import com.crms.mvc.repository.VehicleInspectionRepository;
import com.crms.mvc.repository.VehicleRepository;
import com.crms.mvc.service.VehicleInspectionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class VehicleInspectionServiceImpl implements VehicleInspectionService {

    private final VehicleInspectionRepository vehicleInspectionRepository;
    private final VehicleRepository vehicleRepository;
    private final LicensePlateRepository licensePlateRepository;

    public VehicleInspectionServiceImpl(VehicleInspectionRepository vehicleInspectionRepository,
                                       VehicleRepository vehicleRepository,
                                       LicensePlateRepository licensePlateRepository) {
        this.vehicleInspectionRepository = vehicleInspectionRepository;
        this.vehicleRepository = vehicleRepository;
        this.licensePlateRepository = licensePlateRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleInspection> findAll() {
        return vehicleInspectionRepository.findAllByOrderByScheduledAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VehicleInspection> findById(Long id) {
        return vehicleInspectionRepository.findByIdWithDetails(id);
    }

    @Override
    public VehicleInspection schedule(VehicleInspectionFormDTO dto) {
        Vehicle vehicle = vehicleRepository.findByIdWithDetails(dto.getVehicleId())
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with ID: " + dto.getVehicleId()));

        VehicleInspection inspection = new VehicleInspection();
        inspection.setVehicle(vehicle);

        OffsetDateTime scheduledAt = dto.getScheduledAt() != null
                ? dto.getScheduledAt().atOffset(ZoneOffset.ofHours(7))
                : OffsetDateTime.now(ZoneOffset.ofHours(7));
        inspection.setScheduledAt(scheduledAt);
        inspection.setStatus("scheduled");
        inspection.setNotes(dto.getNotes());

        return vehicleInspectionRepository.save(inspection);
    }

    @Override
    public void startInspection(Long inspectionId) {
        VehicleInspection inspection = vehicleInspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection schedule not found with ID: " + inspectionId));

        Vehicle vehicle = inspection.getVehicle();
        if ("rented".equalsIgnoreCase(vehicle.getStatus())) {
            throw new IllegalStateException("Cannot inspect vehicle while it is currently rented!");
        }

        vehicle.setStatus("maintenance");
        vehicleRepository.save(vehicle);
    }

    @Override
    public void recordResult(Long inspectionId, String status, String notes, LocalDate newRegistrationExpiry) {
        VehicleInspection inspection = vehicleInspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection schedule not found with ID: " + inspectionId));

        if (!"passed".equalsIgnoreCase(status) && !"failed".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("Inspection result must be either 'passed' or 'failed'");
        }

        inspection.setStatus(status.toLowerCase());
        inspection.setCompletedAt(OffsetDateTime.now(ZoneOffset.ofHours(7)));
        if (notes != null && !notes.isBlank()) {
            inspection.setNotes(notes.trim());
        }

        Vehicle vehicle = inspection.getVehicle();
        if ("passed".equalsIgnoreCase(status)) {
            // Restore vehicle to available
            vehicle.setStatus("available");

            // Renew registration expiry on license plate if provided
            if (newRegistrationExpiry != null && vehicle.getLicensePlate() != null) {
                vehicle.getLicensePlate().setRegistrationExpiry(newRegistrationExpiry);
                licensePlateRepository.save(vehicle.getLicensePlate());
            }
        } else {
            // Keep in maintenance for repairs
            vehicle.setStatus("maintenance");
        }

        vehicleRepository.save(vehicle);
        vehicleInspectionRepository.save(inspection);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleInspection> findByVehicleId(Long vehicleId) {
        return vehicleInspectionRepository.findByVehicleIdOrderByScheduledAtDesc(vehicleId);
    }
}
