package com.crms.mvc.service;
import com.crms.mvc.dto.VehicleDTO;
import java.util.List;
import java.util.Optional;
public interface VehicleService {
    List<VehicleDTO> search(String keyword);
    Optional<VehicleDTO> findById(Long id);
}
