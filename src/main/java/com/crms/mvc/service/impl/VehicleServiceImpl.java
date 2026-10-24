package com.crms.mvc.service.impl;

import com.crms.mvc.dto.AdminVehicleQueryDTO;
import com.crms.mvc.dto.LicensePlateChangeDTO;
import com.crms.mvc.dto.VehicleDTO;
import com.crms.mvc.dto.VehicleFormDTO;
import com.crms.mvc.dto.VehicleSearchFilterDTO;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.entity.VehicleType;
import com.crms.mvc.repository.*;
import com.crms.mvc.service.LicensePlateService;
import com.crms.mvc.service.VehicleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final LicensePlateService licensePlateService;
    private final BookingRepository bookingRepository;
    private final VehicleInspectionRepository vehicleInspectionRepository;
    private final IssueRepository issueRepository;

    public VehicleServiceImpl(VehicleRepository vehicleRepository,
                              VehicleTypeRepository vehicleTypeRepository,
                              LicensePlateService licensePlateService,
                              BookingRepository bookingRepository,
                              VehicleInspectionRepository vehicleInspectionRepository,
                              IssueRepository issueRepository) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.licensePlateService = licensePlateService;
        this.bookingRepository = bookingRepository;
        this.vehicleInspectionRepository = vehicleInspectionRepository;
        this.issueRepository = issueRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleDTO> search(String keyword) {
        String query = keyword == null ? "" : keyword.trim();
        return vehicleRepository.findByVehicleTypeBrandContainingIgnoreCaseOrVehicleTypeNameContainingIgnoreCaseOrderByIdAsc(query, query)
                .stream().map(VehicleDTO::fromEntity).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleDTO> searchWithFilter(VehicleSearchFilterDTO filter) {
        List<com.crms.mvc.entity.VehicleType> raw = vehicleTypeRepository.findAll();

        if (filter != null) {
            String keyword = (filter.getKeyword() != null && !filter.getKeyword().isBlank()) ? filter.getKeyword().trim().toLowerCase() : null;
            String brand = (filter.getBrand() != null && !filter.getBrand().isBlank() && !"all".equalsIgnoreCase(filter.getBrand())) ? filter.getBrand().trim() : null;
            String category = (filter.getCategory() != null && !filter.getCategory().isBlank() && !"all".equalsIgnoreCase(filter.getCategory())) ? filter.getCategory().trim() : null;
            Integer seats = (filter.getSeats() != null && filter.getSeats() > 0) ? filter.getSeats() : null;
            String transmission = (filter.getTransmission() != null && !filter.getTransmission().isBlank() && !"all".equalsIgnoreCase(filter.getTransmission())) ? filter.getTransmission().trim() : null;
            String fuelType = (filter.getFuelType() != null && !filter.getFuelType().isBlank() && !"all".equalsIgnoreCase(filter.getFuelType())) ? filter.getFuelType().trim() : null;
            BigDecimal minPrice = filter.getMinPrice();
            BigDecimal maxPrice = filter.getMaxPrice();

            raw = raw.stream()
                    .filter(vt -> keyword == null ||
                            (vt.getName() != null && vt.getName().toLowerCase().contains(keyword)) ||
                            (vt.getBrand() != null && vt.getBrand().toLowerCase().contains(keyword)))
                    .filter(vt -> brand == null || (vt.getBrand() != null && brand.equalsIgnoreCase(vt.getBrand())))
                    .filter(vt -> category == null || (vt.getCategory() != null && category.equalsIgnoreCase(vt.getCategory())))
                    .filter(vt -> seats == null || (vt.getSeats() != null && vt.getSeats() >= seats))
                    .filter(vt -> transmission == null || (vt.getTransmission() != null && transmission.equalsIgnoreCase(vt.getTransmission())))
                    .filter(vt -> fuelType == null || (vt.getFuelType() != null && fuelType.equalsIgnoreCase(vt.getFuelType())))
                    .filter(vt -> minPrice == null || (vt.getPricePerHour() != null && vt.getPricePerHour().compareTo(minPrice) >= 0))
                    .filter(vt -> maxPrice == null || (vt.getPricePerHour() != null && vt.getPricePerHour().compareTo(maxPrice) <= 0))
                    .toList();
        }

        // Sorting. Names are compared the way customers read them on the card: brand + model.
        String sortBy = (filter != null && filter.getSortBy() != null) ? filter.getSortBy() : "price_asc";
        Comparator<com.crms.mvc.entity.VehicleType> byName =
                Comparator.comparing(com.crms.mvc.entity.VehicleType::getDisplayName, String.CASE_INSENSITIVE_ORDER);
        Comparator<com.crms.mvc.entity.VehicleType> comparator = switch (sortBy) {
            case "price_desc" -> Comparator.comparing(com.crms.mvc.entity.VehicleType::getPricePerHour, Comparator.nullsLast(Comparator.reverseOrder()));
            case "name_asc" -> byName;
            case "name_desc" -> byName.reversed();
            default -> Comparator.comparing(com.crms.mvc.entity.VehicleType::getPricePerHour, Comparator.nullsLast(Comparator.naturalOrder()));
        };

        return raw.stream().sorted(comparator).map(VehicleDTO::fromVehicleType).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VehicleDTO> findById(Long id) {
        Optional<com.crms.mvc.entity.VehicleType> vt = vehicleTypeRepository.findById(id);
        if (vt.isPresent()) {
            return vt.map(VehicleDTO::fromVehicleType);
        }
        return vehicleRepository.findByIdWithDetails(id).map(VehicleDTO::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Vehicle> findAll() {
        return vehicleRepository.findAllWithDetails();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehicleDTO> searchForAdmin(AdminVehicleQueryDTO query) {
        Sort sort = adminSort(query.getSort());
        String pattern = query.getLikePattern();

        Page<Vehicle> page = vehicleRepository.searchForAdmin(query.getStatus(), pattern,
                PageRequest.of(query.getPageIndex(), query.getSize(), sort));
        if (page.isEmpty() && page.getTotalPages() > 0) {
            // The requested page no longer exists (for example the last car on it was just deleted)
            page = vehicleRepository.searchForAdmin(query.getStatus(), pattern,
                    PageRequest.of(page.getTotalPages() - 1, query.getSize(), sort));
        }
        return page.map(VehicleDTO::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countByStatus() {
        Map<String, Long> counts = new HashMap<>();
        vehicleRepository.countGroupedByStatus().forEach(row -> counts.put(row.getStatus(), row.getTotal()));
        return counts;
    }

    /** Whitelist of sort keys; the id at the end keeps the order stable when names or plates are equal. */
    private Sort adminSort(String sortKey) {
        return switch (sortKey) {
            case AdminVehicleQueryDTO.SORT_PLATE_ASC -> Sort.by(Sort.Order.asc("lp.plate").ignoreCase(), Sort.Order.asc("id"));
            case AdminVehicleQueryDTO.SORT_NAME_DESC -> Sort.by(
                    Sort.Order.desc("vt.brand").ignoreCase(), Sort.Order.desc("vt.name").ignoreCase(), Sort.Order.asc("id"));
            default -> Sort.by(
                    Sort.Order.asc("vt.brand").ignoreCase(), Sort.Order.asc("vt.name").ignoreCase(), Sort.Order.asc("id"));
        };
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Vehicle> findEntityById(Long id) {
        return vehicleRepository.findByIdWithDetails(id);
    }

    @Override
    public Vehicle create(VehicleFormDTO dto) {
        VehicleType vt = vehicleTypeRepository.findById(dto.getVehicleTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Vehicle model not found with ID: " + dto.getVehicleTypeId()));

        String cleanPlate = dto.getPlate().trim().toUpperCase();
        if (licensePlateService.isPlateExists(cleanPlate)) {
            throw new IllegalArgumentException("License plate " + cleanPlate
                    + " has already been issued to a vehicle; a retired plate cannot be reused");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleType(vt);
        vehicle.setColor(dto.getColor().trim());
        vehicle.setYear(dto.getYear());
        vehicle.setOdometer(dto.getOdometer() != null ? dto.getOdometer() : 0);
        vehicle.setRegistrationExpiry(dto.getRegistrationExpiry());
        vehicle.setStatus(dto.getStatus() != null ? dto.getStatus().trim().toLowerCase() : "available");
        Vehicle saved = vehicleRepository.save(vehicle);

        // First plate of the vehicle's history; later changes go through LicensePlateService.changePlate.
        LicensePlateChangeDTO firstPlate = new LicensePlateChangeDTO();
        firstPlate.setPlate(cleanPlate);
        firstPlate.setProvince(dto.getProvince());
        licensePlateService.changePlate(saved.getId(), firstPlate);
        return saved;
    }

    @Override
    public Vehicle update(Long id, VehicleFormDTO dto) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with ID: " + id));

        VehicleType vt = vehicleTypeRepository.findById(dto.getVehicleTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Vehicle model not found with ID: " + dto.getVehicleTypeId()));

        vehicle.setVehicleType(vt);
        vehicle.setColor(dto.getColor().trim());
        vehicle.setYear(dto.getYear());
        vehicle.setOdometer(dto.getOdometer() != null ? dto.getOdometer() : 0);

        // Don't allow changing status to maintenance/available if vehicle is currently rented
        if ("rented".equalsIgnoreCase(vehicle.getStatus()) && !vehicle.getStatus().equalsIgnoreCase(dto.getStatus())) {
            throw new IllegalStateException("Cannot change status of vehicle while currently rented!");
        }
        if (dto.getStatus() != null) {
            vehicle.setStatus(dto.getStatus().trim().toLowerCase());
        }

        // The plate is not edited here: changing it creates a new plate record (LicensePlateService.changePlate).
        vehicle.setRegistrationExpiry(dto.getRegistrationExpiry());

        return vehicleRepository.save(vehicle);
    }

    @Override
    public void updateStatus(Long id, String newStatus) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with ID: " + id));

        if ("rented".equalsIgnoreCase(vehicle.getStatus())) {
            throw new IllegalStateException("Cannot change status of vehicle while currently rented!");
        }

        vehicle.setStatus(newStatus.trim().toLowerCase());
        vehicleRepository.save(vehicle);
    }

    @Override
    public void updateOdometer(Long id, Integer newOdometer) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with ID: " + id));

        if (newOdometer != null && newOdometer >= 0) {
            vehicle.setOdometer(newOdometer);
            vehicleRepository.save(vehicle);
        }
    }

    @Override
    public void updateRegistrationExpiry(Long id, LocalDate registrationExpiry) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with ID: " + id));
        if (registrationExpiry == null) {
            throw new IllegalArgumentException("Registration expiry date is required");
        }
        vehicle.setRegistrationExpiry(registrationExpiry);
        vehicleRepository.save(vehicle);
    }

    @Override
    public void delete(Long id) {
        long bookingCount = bookingRepository.countByVehicleId(id);
        if (bookingCount > 0) {
            throw new IllegalStateException("Cannot delete vehicle because " + bookingCount + " bookings are linked to it!");
        }

        long inspectionCount = vehicleInspectionRepository.countByVehicleId(id);
        if (inspectionCount > 0) {
            throw new IllegalStateException("Cannot delete vehicle because " + inspectionCount + " inspections are linked to it!");
        }

        long issueCount = issueRepository.countByVehicleId(id);
        if (issueCount > 0) {
            throw new IllegalStateException("Cannot delete vehicle because " + issueCount + " issues are linked to it!");
        }

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with ID: " + id));

        vehicleRepository.delete(vehicle);
    }
}
