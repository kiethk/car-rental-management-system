package com.crms.mvc.service.impl;

import com.crms.mvc.dto.LicensePlateOverviewDTO;
import com.crms.mvc.entity.LicensePlate;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.repository.LicensePlateRepository;
import com.crms.mvc.repository.VehicleRepository;
import com.crms.mvc.service.LicensePlateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
        List<LicensePlate> plates = licensePlateRepository.findAllByOrderByIdAsc();
        return plates.stream().map(lp -> {
            Optional<Vehicle> vOpt = vehicleRepository.findByLicensePlateId(lp.getId());
            Long vehicleId = vOpt.map(Vehicle::getId).orElse(null);
            String vehicleName = vOpt.map(v -> v.getVehicleType() != null 
                    ? v.getVehicleType().getBrand() + " " + v.getVehicleType().getName()
                    : "Chưa rõ dòng xe").orElse("Chưa gán xe");
            String vehicleStatus = vOpt.map(Vehicle::getStatus).orElse("unassigned");

            return new LicensePlateOverviewDTO(
                    lp.getId(),
                    lp.getPlate(),
                    lp.getProvince(),
                    lp.getRegistrationExpiry(),
                    vehicleId,
                    vehicleName,
                    vehicleStatus
            );
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LicensePlate> findById(Long id) {
        return licensePlateRepository.findById(id);
    }

    @Override
    public LicensePlate update(Long id, String province, LocalDate registrationExpiry) {
        LicensePlate lp = licensePlateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biển số ID: " + id));
        if (province != null) {
            lp.setProvince(province.trim());
        }
        if (registrationExpiry != null) {
            lp.setRegistrationExpiry(registrationExpiry);
        }
        return licensePlateRepository.save(lp);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LicensePlate> findUnassignedPlates() {
        return licensePlateRepository.findUnassignedPlates();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPlateExists(String plate, Long excludeId) {
        if (plate == null || plate.isBlank()) return false;
        String clean = plate.trim();
        if (excludeId == null) {
            return licensePlateRepository.existsByPlateIgnoreCase(clean);
        } else {
            return licensePlateRepository.existsByPlateIgnoreCaseAndIdNot(clean, excludeId);
        }
    }
}
