package com.crms.mvc.service.impl;

import com.crms.mvc.dto.PageQueryDTO;
import com.crms.mvc.dto.VehicleTypeFormDTO;
import com.crms.mvc.entity.VehicleType;
import com.crms.mvc.repository.SlotRepository;
import com.crms.mvc.repository.VehicleRepository;
import com.crms.mvc.repository.VehicleTypeRepository;
import com.crms.mvc.service.FileUploadService;
import com.crms.mvc.service.VehicleTypeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    public Page<VehicleType> searchForAdmin(PageQueryDTO query) {
        Sort.Direction direction = PageQueryDTO.SORT_NAME_DESC.equals(query.getSort())
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        // Alphabetical by what the admin reads: brand first, then model; id keeps the order stable
        Sort sort = Sort.by(
                new Sort.Order(direction, "brand").ignoreCase(),
                new Sort.Order(direction, "name").ignoreCase(),
                Sort.Order.asc("id"));
        String pattern = query.getLikePattern();

        Page<VehicleType> page = vehicleTypeRepository.searchForAdmin(pattern,
                PageRequest.of(query.getPageIndex(), query.getSize(), sort));
        if (page.isEmpty() && page.getTotalPages() > 0) {
            // The requested page no longer exists (for example the last item was just deleted)
            page = vehicleTypeRepository.searchForAdmin(pattern,
                    PageRequest.of(page.getTotalPages() - 1, query.getSize(), sort));
        }
        return page;
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
                .orElseThrow(() -> new IllegalArgumentException("Vehicle model not found with ID: " + id));

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
            throw new IllegalStateException("Cannot delete car model because " + vehicleCount + " physical vehicles are linked to it!");
        }

        long slotCount = slotRepository.countByVehicleTypeId(id);
        if (slotCount > 0) {
            throw new IllegalStateException("Cannot delete car model because " + slotCount + " slots are linked to it!");
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
