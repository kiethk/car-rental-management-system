package com.crms.mvc.service;

import com.crms.mvc.dto.VehicleDTO;
import com.crms.mvc.dto.VehicleFormDTO;
import com.crms.mvc.dto.VehicleSearchFilterDTO;
import com.crms.mvc.entity.Vehicle;

import java.util.List;
import java.util.Optional;

public interface VehicleService {
    List<VehicleDTO> search(String keyword);

    List<VehicleDTO> searchWithFilter(VehicleSearchFilterDTO filter);

    Optional<VehicleDTO> findById(Long id);

    List<Vehicle> findAll();

    Optional<Vehicle> findEntityById(Long id);

    Vehicle create(VehicleFormDTO dto);

    Vehicle update(Long id, VehicleFormDTO dto);

    void updateStatus(Long id, String newStatus);

    void updateOdometer(Long id, Integer newOdometer);

    void delete(Long id);
}
