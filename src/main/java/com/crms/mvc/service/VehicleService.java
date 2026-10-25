package com.crms.mvc.service;

import com.crms.mvc.dto.AdminVehicleQueryDTO;
import com.crms.mvc.dto.VehicleDTO;
import com.crms.mvc.dto.VehicleFormDTO;
import com.crms.mvc.dto.VehicleSearchFilterDTO;
import com.crms.mvc.entity.Vehicle;

import org.springframework.data.domain.Page;

import java.time.LocalDate;
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

    Optional<Vehicle> findEntityById(Long id);

    Vehicle create(VehicleFormDTO dto);

    Vehicle update(Long id, VehicleFormDTO dto);

    void updateStatus(Long id, String newStatus);

    void updateOdometer(Long id, Integer newOdometer);

    /** Registration (inspection sticker) belongs to the vehicle, so it survives plate changes. */
    void updateRegistrationExpiry(Long id, LocalDate registrationExpiry);

    List<Vehicle> findByVehicleTypeIdWithDetails(Long typeId);

    List<Vehicle> findByStatusWithDetails(String status);

    void delete(Long id);
}
