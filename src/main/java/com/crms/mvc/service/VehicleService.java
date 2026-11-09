package com.crms.mvc.service;

import com.crms.mvc.enums.VehicleStatus;
import com.crms.mvc.dto.AdminVehicleQueryDTO;
import com.crms.mvc.dto.VehicleDTO;
import com.crms.mvc.dto.VehicleFormDTO;
import com.crms.mvc.dto.VehicleSearchFilterDTO;
import com.crms.mvc.entity.Vehicle;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface VehicleService {
    List<VehicleDTO> search(String keyword);

    List<VehicleDTO> searchWithFilter(VehicleSearchFilterDTO filter);

    Optional<VehicleDTO> findById(Long id);

    List<Vehicle> findAll();

    /** One page of cars for the admin list: filtered by status and keyword, sorted alphabetically. */
    Page<VehicleDTO> searchForAdmin(AdminVehicleQueryDTO query);

    /** Number of cars per status (available, rented, maintenance); statuses with no car are absent. */
    Map<String, Long> countByStatus();

    /** One car (never a vehicle type: the two have separate ids) for the admin pages; empty when there is none. */
    Optional<VehicleDTO> findVehicleById(Long id);

    /** The edit form of one car, filled with what it holds now. */
    Optional<VehicleFormDTO> findFormById(Long id);

    VehicleDTO create(VehicleFormDTO dto);

    VehicleDTO update(Long id, VehicleFormDTO dto);

    void updateStatus(Long id, String newStatus);

    void updateOdometer(Long id, Integer newOdometer);

    /** Registration (inspection sticker) belongs to the vehicle, so it survives plate changes. */

    List<Vehicle> findByVehicleTypeIdWithDetails(Long typeId);

    List<Vehicle> findByStatusWithDetails(VehicleStatus status);

    void delete(Long id);
}
