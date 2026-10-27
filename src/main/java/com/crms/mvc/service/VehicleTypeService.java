package com.crms.mvc.service;

import com.crms.mvc.dto.PageQueryDTO;
import com.crms.mvc.dto.VehicleTypeFormDTO;
import com.crms.mvc.entity.VehicleType;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface VehicleTypeService {
    List<VehicleType> findAll();

    /** One page of vehicle models for the admin list: filtered by keyword, sorted alphabetically. */
    Page<VehicleType> searchForAdmin(PageQueryDTO query);

    Optional<VehicleType> findById(Long id);

    VehicleType create(VehicleTypeFormDTO dto);

    VehicleType update(Long id, VehicleTypeFormDTO dto);

    void delete(Long id);

    List<String> getDistinctBrands();

    List<String> getDistinctCategories();

    long countVehiclesOfType(Long vehicleTypeId);
}
