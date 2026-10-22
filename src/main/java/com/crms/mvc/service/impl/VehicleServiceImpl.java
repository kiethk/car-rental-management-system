package com.crms.mvc.service.impl;

import com.crms.mvc.dto.VehicleDTO;
import com.crms.mvc.dto.VehicleFormDTO;
import com.crms.mvc.dto.VehicleSearchFilterDTO;
import com.crms.mvc.entity.LicensePlate;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.entity.VehicleType;
import com.crms.mvc.repository.*;
import com.crms.mvc.service.VehicleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final LicensePlateRepository licensePlateRepository;
    private final BookingRepository bookingRepository;
    private final VehicleInspectionRepository vehicleInspectionRepository;
    private final IssueRepository issueRepository;

    public VehicleServiceImpl(VehicleRepository vehicleRepository,
                              VehicleTypeRepository vehicleTypeRepository,
                              LicensePlateRepository licensePlateRepository,
                              BookingRepository bookingRepository,
                              VehicleInspectionRepository vehicleInspectionRepository,
                              IssueRepository issueRepository) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.licensePlateRepository = licensePlateRepository;
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
        List<Vehicle> raw = vehicleRepository.findAllWithDetails();

        String status = (filter != null && filter.getStatus() != null && !filter.getStatus().isBlank())
                ? filter.getStatus().trim()
                : "available";

        // Customer fleet browse/search strictly defaults to 'available' vehicles
        raw = raw.stream()
                .filter(v -> v.getVehicleType() != null)
                .filter(v -> "all".equalsIgnoreCase(status) || (v.getStatus() != null && status.equalsIgnoreCase(v.getStatus())))
                .toList();

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
                    .filter(v -> keyword == null ||
                            (v.getVehicleType().getName() != null && v.getVehicleType().getName().toLowerCase().contains(keyword)) ||
                            (v.getVehicleType().getBrand() != null && v.getVehicleType().getBrand().toLowerCase().contains(keyword)))
                    .filter(v -> brand == null || (v.getVehicleType().getBrand() != null && brand.equalsIgnoreCase(v.getVehicleType().getBrand())))
                    .filter(v -> category == null || (v.getVehicleType().getCategory() != null && category.equalsIgnoreCase(v.getVehicleType().getCategory())))
                    .filter(v -> seats == null || (v.getVehicleType().getSeats() != null && v.getVehicleType().getSeats() >= seats))
                    .filter(v -> transmission == null || (v.getVehicleType().getTransmission() != null && transmission.equalsIgnoreCase(v.getVehicleType().getTransmission())))
                    .filter(v -> fuelType == null || (v.getVehicleType().getFuelType() != null && fuelType.equalsIgnoreCase(v.getVehicleType().getFuelType())))
                    .filter(v -> minPrice == null || (v.getVehicleType().getPricePerHour() != null && v.getVehicleType().getPricePerHour().compareTo(minPrice) >= 0))
                    .filter(v -> maxPrice == null || (v.getVehicleType().getPricePerHour() != null && v.getVehicleType().getPricePerHour().compareTo(maxPrice) <= 0))
                    .toList();
        }

        // Sorting
        String sortBy = (filter != null && filter.getSortBy() != null) ? filter.getSortBy() : "price_asc";
        Comparator<Vehicle> comparator = switch (sortBy) {
            case "price_desc" -> Comparator.comparing((Vehicle v) -> v.getVehicleType().getPricePerHour(), Comparator.nullsLast(Comparator.reverseOrder()));
            case "year_desc" -> Comparator.comparing(Vehicle::getYear, Comparator.nullsLast(Comparator.reverseOrder()));
            case "name_asc" -> Comparator.comparing((Vehicle v) -> v.getVehicleType().getName(), String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing((Vehicle v) -> v.getVehicleType().getPricePerHour(), Comparator.nullsLast(Comparator.naturalOrder()));
        };

        return raw.stream().sorted(comparator).map(VehicleDTO::fromEntity).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VehicleDTO> findById(Long id) {
        return vehicleRepository.findByIdWithDetails(id).map(VehicleDTO::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Vehicle> findAll() {
        return vehicleRepository.findAllWithDetails();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Vehicle> findEntityById(Long id) {
        return vehicleRepository.findByIdWithDetails(id);
    }

    @Override
    public Vehicle create(VehicleFormDTO dto) {
        VehicleType vt = vehicleTypeRepository.findById(dto.getVehicleTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dòng xe ID: " + dto.getVehicleTypeId()));

        // Process 1:1 License Plate
        String cleanPlate = dto.getPlate().trim().toUpperCase();
        LicensePlate plate;
        Optional<LicensePlate> existingPlateOpt = licensePlateRepository.findByPlateIgnoreCase(cleanPlate);

        if (existingPlateOpt.isPresent()) {
            plate = existingPlateOpt.get();
            if (vehicleRepository.existsByLicensePlateId(plate.getId())) {
                throw new IllegalArgumentException("Biển số " + cleanPlate + " đã được gán cho một chiếc xe khác!");
            }
            if (dto.getProvince() != null && !dto.getProvince().isBlank()) {
                plate.setProvince(dto.getProvince().trim());
            }
            if (dto.getRegistrationExpiry() != null) {
                plate.setRegistrationExpiry(dto.getRegistrationExpiry());
            }
            plate = licensePlateRepository.save(plate);
        } else {
            plate = new LicensePlate();
            plate.setPlate(cleanPlate);
            plate.setProvince(dto.getProvince() != null ? dto.getProvince().trim() : null);
            plate.setRegistrationExpiry(dto.getRegistrationExpiry());
            plate = licensePlateRepository.save(plate);
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleType(vt);
        vehicle.setLicensePlate(plate);
        vehicle.setColor(dto.getColor().trim());
        vehicle.setYear(dto.getYear());
        vehicle.setOdometer(dto.getOdometer() != null ? dto.getOdometer() : 0);
        vehicle.setStatus(dto.getStatus() != null ? dto.getStatus().trim().toLowerCase() : "available");

        return vehicleRepository.save(vehicle);
    }

    @Override
    public Vehicle update(Long id, VehicleFormDTO dto) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy xe ID: " + id));

        VehicleType vt = vehicleTypeRepository.findById(dto.getVehicleTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dòng xe ID: " + dto.getVehicleTypeId()));

        vehicle.setVehicleType(vt);
        vehicle.setColor(dto.getColor().trim());
        vehicle.setYear(dto.getYear());
        vehicle.setOdometer(dto.getOdometer() != null ? dto.getOdometer() : 0);

        // Don't allow changing status to maintenance/available if vehicle is currently rented
        if ("rented".equalsIgnoreCase(vehicle.getStatus()) && !vehicle.getStatus().equalsIgnoreCase(dto.getStatus())) {
            throw new IllegalStateException("Không thể đổi trạng thái của xe đang được khách thuê (rented)!");
        }
        if (dto.getStatus() != null) {
            vehicle.setStatus(dto.getStatus().trim().toLowerCase());
        }

        // Update license plate info
        String cleanPlate = dto.getPlate().trim().toUpperCase();
        LicensePlate plate = vehicle.getLicensePlate();

        if (plate == null) {
            Optional<LicensePlate> existing = licensePlateRepository.findByPlateIgnoreCase(cleanPlate);
            if (existing.isPresent() && vehicleRepository.existsByLicensePlateId(existing.get().getId())) {
                throw new IllegalArgumentException("Biển số " + cleanPlate + " đã thuộc về xe khác!");
            }
            plate = existing.orElseGet(() -> {
                LicensePlate newPlate = new LicensePlate();
                newPlate.setPlate(cleanPlate);
                return newPlate;
            });
        } else if (!plate.getPlate().equalsIgnoreCase(cleanPlate)) {
            if (licensePlateRepository.existsByPlateIgnoreCaseAndIdNot(cleanPlate, plate.getId())) {
                throw new IllegalArgumentException("Biển số " + cleanPlate + " đã tồn tại trong hệ thống!");
            }
            plate.setPlate(cleanPlate);
        }

        plate.setProvince(dto.getProvince() != null ? dto.getProvince().trim() : null);
        if (dto.getRegistrationExpiry() != null) {
            plate.setRegistrationExpiry(dto.getRegistrationExpiry());
        }
        plate = licensePlateRepository.save(plate);
        vehicle.setLicensePlate(plate);

        return vehicleRepository.save(vehicle);
    }

    @Override
    public void updateStatus(Long id, String newStatus) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy xe ID: " + id));

        if ("rented".equalsIgnoreCase(vehicle.getStatus())) {
            throw new IllegalStateException("Không thể đổi trạng thái của xe đang được khách thuê (rented)!");
        }

        vehicle.setStatus(newStatus.trim().toLowerCase());
        vehicleRepository.save(vehicle);
    }

    @Override
    public void updateOdometer(Long id, Integer newOdometer) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy xe ID: " + id));

        if (newOdometer != null && newOdometer >= 0) {
            vehicle.setOdometer(newOdometer);
            vehicleRepository.save(vehicle);
        }
    }

    @Override
    public void delete(Long id) {
        long bookingCount = bookingRepository.countByVehicleId(id);
        if (bookingCount > 0) {
            throw new IllegalStateException("Không thể xóa xe vì đã có " + bookingCount + " đơn đặt xe (booking) liên kết!");
        }

        long inspectionCount = vehicleInspectionRepository.countByVehicleId(id);
        if (inspectionCount > 0) {
            throw new IllegalStateException("Không thể xóa xe vì đã có " + inspectionCount + " lịch kiểm định liên kết!");
        }

        long issueCount = issueRepository.countByVehicleId(id);
        if (issueCount > 0) {
            throw new IllegalStateException("Không thể xóa xe vì đã có " + issueCount + " sự cố (issue) liên kết!");
        }

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy xe ID: " + id));

        vehicleRepository.delete(vehicle);
    }
}
