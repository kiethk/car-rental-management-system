package com.crms.mvc.service.impl;

import com.crms.mvc.dto.VehicleInspectionFormDTO;
import com.crms.mvc.entity.Payment;
import com.crms.mvc.entity.Vehicle;
import com.crms.mvc.entity.VehicleInspection;
import com.crms.mvc.repository.BookingRepository;
import com.crms.mvc.repository.VehicleInspectionRepository;
import com.crms.mvc.repository.VehicleRepository;
import com.crms.mvc.service.VehicleInspectionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class VehicleInspectionServiceImpl implements VehicleInspectionService {

    private final VehicleInspectionRepository vehicleInspectionRepository;
    private final VehicleRepository vehicleRepository;
    private final BookingRepository bookingRepository;

    public VehicleInspectionServiceImpl(VehicleInspectionRepository vehicleInspectionRepository,
                                       VehicleRepository vehicleRepository,
                                       BookingRepository bookingRepository) {
        this.vehicleInspectionRepository = vehicleInspectionRepository;
        this.vehicleRepository = vehicleRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleInspection> findAll() {
        return vehicleInspectionRepository.findAllByOrderByScheduledAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VehicleInspection> findById(Long id) {
        return vehicleInspectionRepository.findByIdWithDetails(id);
    }

    @Override
    public VehicleInspection schedule(VehicleInspectionFormDTO dto) {
        Vehicle vehicle = vehicleRepository.findByIdWithDetails(dto.getVehicleId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy xe có mã: " + dto.getVehicleId()));
        requireNotRetired(vehicle);

        if (dto.getScheduledAt() != null && dto.getScheduledAt().isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new IllegalArgumentException("Thời gian lên lịch đăng kiểm không được ở trong quá khứ");
        }

        VehicleInspection inspection = new VehicleInspection();
        inspection.setVehicle(vehicle);

        OffsetDateTime scheduledAt = dto.getScheduledAt() != null
                ? dto.getScheduledAt().atOffset(ZoneOffset.ofHours(7))
                : OffsetDateTime.now(ZoneOffset.ofHours(7));
        requireNoRentalOnThatDay(vehicle, scheduledAt);
        inspection.setScheduledAt(scheduledAt);
        inspection.setStatus("scheduled");
        inspection.setNotes(dto.getNotes());

        return vehicleInspectionRepository.save(inspection);
    }

    @Override
    public void startInspection(Long inspectionId) {
        VehicleInspection inspection = vehicleInspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lịch đăng kiểm có mã: " + inspectionId));

        Vehicle vehicle = inspection.getVehicle();
        requireNotRetired(vehicle);
        if (Vehicle.STATUS_RENTED.equalsIgnoreCase(vehicle.getStatus())) {
            throw new IllegalStateException("Không đăng kiểm được khi xe đang cho thuê!");
        }

        vehicle.setStatus(Vehicle.STATUS_MAINTENANCE);
        vehicleRepository.save(vehicle);
    }

    @Override
    public void recordResult(Long inspectionId, String status, String notes, LocalDate newRegistrationExpiry) {
        VehicleInspection inspection = vehicleInspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lịch đăng kiểm có mã: " + inspectionId));

        if (!"passed".equalsIgnoreCase(status) && !"failed".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("Kết quả đăng kiểm phải là đạt hoặc không đạt");
        }
        requireNotRetired(inspection.getVehicle());

        if ("passed".equalsIgnoreCase(status) && newRegistrationExpiry != null && newRegistrationExpiry.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Hạn đăng kiểm mới không được ở trong quá khứ");
        }

        inspection.setStatus(status.toLowerCase());
        inspection.setCompletedAt(OffsetDateTime.now(ZoneOffset.ofHours(7)));
        if (notes != null && !notes.isBlank()) {
            inspection.setNotes(notes.trim());
        }

        Vehicle vehicle = inspection.getVehicle();
        if ("passed".equalsIgnoreCase(status)) {
            // Restore vehicle to available
            vehicle.setStatus(Vehicle.STATUS_AVAILABLE);

            // The new inspection sticker belongs to the vehicle, whatever plate it carries
            if (newRegistrationExpiry != null) {
                vehicle.setRegistrationExpiry(newRegistrationExpiry);
            }
        } else {
            // Keep in maintenance for repairs
            vehicle.setStatus(Vehicle.STATUS_MAINTENANCE);
        }

        vehicleRepository.save(vehicle);
        vehicleInspectionRepository.save(inspection);
    }

    /** The whole day is reserved for the inspection, so the car must not already be promised to a customer that day. */
    private void requireNoRentalOnThatDay(Vehicle vehicle, OffsetDateTime scheduledAt) {
        OffsetDateTime dayStart = scheduledAt.toLocalDate().atStartOfDay().atOffset(scheduledAt.getOffset());
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.ofHours(7));
        List<Long> bookedVehicleIds = bookingRepository.findBookedVehicleIdsInTimeRange(
                dayStart, dayStart.plusDays(1), now.minusMinutes(Payment.PAYMENT_TIMEOUT_MINUTES), now);
        if (bookedVehicleIds.contains(vehicle.getId())) {
            throw new IllegalStateException("Xe này đã có đơn thuê trong ngày " + scheduledAt.toLocalDate()
                    + ", không thể lên lịch đăng kiểm vào ngày này. Hãy chọn ngày khác.");
        }
    }

    /** A retired vehicle is out of service for good; inspections must not bring it back. */
    private void requireNotRetired(Vehicle vehicle) {
        if (vehicle.isRetired()) {
            throw new IllegalStateException("Xe đã ngừng khai thác nên không thể đăng kiểm nữa");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleInspection> findByVehicleId(Long vehicleId) {
        return vehicleInspectionRepository.findByVehicleIdOrderByScheduledAtDesc(vehicleId);
    }
}
