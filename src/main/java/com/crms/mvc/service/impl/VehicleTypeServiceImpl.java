package com.crms.mvc.service.impl;

import com.crms.mvc.dto.VehicleTypeFormDTO;
import com.crms.mvc.entity.VehicleType;
import com.crms.mvc.repository.SlotRepository;
import com.crms.mvc.repository.VehicleRepository;
import com.crms.mvc.repository.VehicleTypeRepository;
import com.crms.mvc.service.FileUploadService;
import com.crms.mvc.service.VehicleTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class VehicleTypeServiceImpl implements VehicleTypeService {

    private final VehicleTypeRepository vehicleTypeRepository;
    private final VehicleRepository vehicleRepository;
    private final SlotRepository slotRepository;
    private final FileUploadService fileUploadService;

    public VehicleTypeServiceImpl(VehicleTypeRepository vehicleTypeRepository,
                                  VehicleRepository vehicleRepository,
                                  SlotRepository slotRepository,
                                  FileUploadService fileUploadService) {
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.vehicleRepository = vehicleRepository;
        this.slotRepository = slotRepository;
        this.fileUploadService = fileUploadService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleType> findAll() {
        return vehicleTypeRepository.findAllByOrderByIdAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VehicleType> findById(Long id) {
        return vehicleTypeRepository.findById(id);
    }

    @Override
    public VehicleType create(VehicleTypeFormDTO dto) {
        VehicleType vt = new VehicleType();
        vt.setName(dto.getName().trim());
        vt.setBrand(dto.getBrand().trim());
        vt.setCategory(dto.getCategory().trim().toLowerCase());
        vt.setSeats(dto.getSeats());
        vt.setTransmission(dto.getTransmission().trim().toLowerCase());
        vt.setFuelType(dto.getFuelType().trim().toLowerCase());
        vt.setPricePerHour(dto.getPricePerHour());
        vt.setDescription(dto.getDescription());

        // Handle image: priority to uploaded file, fallback to image URL
        if (dto.getImageFile() != null && !dto.getImageFile().isEmpty()) {
            String uploadedUrl = fileUploadService.saveVehicleImage(dto.getImageFile());
            vt.setImageUrl(uploadedUrl);
        } else if (dto.getImageUrl() != null && !dto.getImageUrl().isBlank()) {
            vt.setImageUrl(dto.getImageUrl().trim());
        }

        return vehicleTypeRepository.save(vt);
    }

    @Override
    public VehicleType update(Long id, VehicleTypeFormDTO dto) {
        VehicleType vt = vehicleTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dòng xe ID: " + id));

        vt.setName(dto.getName().trim());
        vt.setBrand(dto.getBrand().trim());
        vt.setCategory(dto.getCategory().trim().toLowerCase());
        vt.setSeats(dto.getSeats());
        vt.setTransmission(dto.getTransmission().trim().toLowerCase());
        vt.setFuelType(dto.getFuelType().trim().toLowerCase());
        vt.setPricePerHour(dto.getPricePerHour());
        vt.setDescription(dto.getDescription());

        if (dto.getImageFile() != null && !dto.getImageFile().isEmpty()) {
            String uploadedUrl = fileUploadService.saveVehicleImage(dto.getImageFile());
            vt.setImageUrl(uploadedUrl);
        } else if (dto.getImageUrl() != null && !dto.getImageUrl().isBlank()) {
            vt.setImageUrl(dto.getImageUrl().trim());
        }

        return vehicleTypeRepository.save(vt);
    }

    @Override
    public void delete(Long id) {
        long vehicleCount = vehicleRepository.countByVehicleTypeId(id);
        if (vehicleCount > 0) {
            throw new IllegalStateException("Không thể xóa dòng xe vì đang có " + vehicleCount + " xe vật lý liên kết!");
        }

        long slotCount = slotRepository.countByVehicleTypeId(id);
        if (slotCount > 0) {
            throw new IllegalStateException("Không thể xóa dòng xe vì đang có " + slotCount + " khung giờ (slot) liên kết!");
        }

        vehicleTypeRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getDistinctBrands() {
        return vehicleTypeRepository.findDistinctBrands();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getDistinctCategories() {
        return vehicleTypeRepository.findDistinctCategories();
    }

    @Override
    @Transactional(readOnly = true)
    public long countVehiclesOfType(Long vehicleTypeId) {
        return vehicleRepository.countByVehicleTypeId(vehicleTypeId);
    }
}
