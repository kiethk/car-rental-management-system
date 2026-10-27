package com.crms.mvc.service.impl;

import com.crms.mvc.dto.LicensePlateChangeDTO;
import com.crms.mvc.dto.LicensePlateDTO;
import com.crms.mvc.dto.LicensePlateOverviewDTO;
import com.crms.mvc.entity.LicensePlate;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.repository.LicensePlateRepository;
import com.crms.mvc.repository.VehicleRepository;
import com.crms.mvc.service.LicensePlateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class LicensePlateServiceImpl implements LicensePlateService {

    private final LicensePlateRepository licensePlateRepository;
    private final VehicleRepository vehicleRepository;

    public LicensePlateServiceImpl(LicensePlateRepository licensePlateRepository,
                                   VehicleRepository vehicleRepository) {
        this.licensePlateRepository = licensePlateRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LicensePlateOverviewDTO> findAllOverviews() {
        return vehicleRepository.findAllWithDetails().stream().map(vehicle -> {
            LicensePlate current = vehicle.getLicensePlate();
            return new LicensePlateOverviewDTO(
                    vehicle.getId(),
                    vehicle.getVehicleType().getDisplayName(),
                    vehicle.getStatus(),
                    current != null ? current.getPlate() : null,
                    current != null ? current.getProvince() : null,
                    current != null ? current.getValidFrom() : null,
                    vehicle.getRegistrationExpiry());
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LicensePlateDTO> findHistory(Long vehicleId) {
        return licensePlateRepository.findByVehicleIdOrderByValidFromDesc(vehicleId).stream()
                .map(LicensePlateDTO::fromEntity)
                .toList();
    }

    @Override
    public LicensePlateDTO changePlate(Long vehicleId, LicensePlateChangeDTO form) {
        Vehicle vehicle = vehicleRepository.findByIdWithDetails(vehicleId)
                .orElseThrow(() -> new NoSuchElementException("Vehicle not found: " + vehicleId));
        LicensePlate current = vehicle.getLicensePlate();
        String newPlate = form.getPlate().trim().toUpperCase();

        if (current != null) {
            if ("rented".equalsIgnoreCase(vehicle.getStatus())) {
                throw new IllegalStateException("Cannot change the plate while the vehicle is rented out");
            }
            if (current.getPlate().equalsIgnoreCase(newPlate)) {
                throw new IllegalArgumentException("Plate " + newPlate + " is already on this vehicle");
            }
        }
        if (licensePlateRepository.existsByPlateIgnoreCase(newPlate)) {
            throw new IllegalArgumentException("Plate " + newPlate
                    + " has already been issued to a vehicle; a retired plate cannot be reused");
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (current != null) {
            current.setValidTo(now);
            // Flush first: the database allows only one open-ended plate per vehicle at any moment.
            licensePlateRepository.saveAndFlush(current);
        }

        LicensePlate next = new LicensePlate();
        next.setVehicle(vehicle);
        next.setPlate(newPlate);
        next.setProvince(form.getProvince() != null && !form.getProvince().isBlank() ? form.getProvince().trim() : null);
        next.setValidFrom(now);
        LicensePlate saved = licensePlateRepository.save(next);
        vehicle.setCurrentPlate(saved);
        return LicensePlateDTO.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPlateExists(String plate) {
        return plate != null && !plate.isBlank() && licensePlateRepository.existsByPlateIgnoreCase(plate.trim());
    }
}
