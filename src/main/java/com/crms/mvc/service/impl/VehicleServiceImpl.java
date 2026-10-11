package com.crms.mvc.service.impl;
import com.crms.mvc.dto.VehicleDTO;
import com.crms.mvc.repository.VehicleRepository;
import com.crms.mvc.service.VehicleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class VehicleServiceImpl implements VehicleService {
    private final VehicleRepository vehicleRepository;
    public VehicleServiceImpl(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }
    public List<VehicleDTO> search(String keyword) {
        String query = keyword == null ? "" : keyword.trim();
        return vehicleRepository.findByVehicleTypeBrandContainingIgnoreCaseOrVehicleTypeNameContainingIgnoreCaseOrderByIdAsc(query, query)
                .stream().map(VehicleDTO::fromEntity).toList();
    }
    public Optional<VehicleDTO> findById(Long id) {
        return vehicleRepository.findById(id).map(VehicleDTO::fromEntity);
    }
}
