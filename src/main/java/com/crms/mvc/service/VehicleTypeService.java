package com.crms.mvc.service;

import com.crms.mvc.dto.VehicleTypeFormDTO;
import com.crms.mvc.entity.VehicleType;

import java.util.List;
import java.util.Optional;

public interface VehicleTypeService {
    List<VehicleType> findAll();

    Optional<VehicleType> findById(Long id);

    VehicleType create(VehicleTypeFormDTO dto);

    VehicleType update(Long id, VehicleTypeFormDTO dto);

    void delete(Long id);

    List<String> getDistinctBrands();

    List<String> getDistinctCategories();

    long countVehiclesOfType(Long vehicleTypeId);
}
